//firebase functions:config:set stripe.sk=<YOUR STRIPE SECRET KEY>
//firebase functions:config:set stripe.pk=<YOUR STRIPE PUBLISHABLE KEY>

'use strict';
const REQUEST_TIMEOUT_SECONDS = 20;
const STRIPE_ENABLED = true
const functions = require('firebase-functions');
const admin = require('firebase-admin');
const geofire = require('geofire');
const stripe = require("stripe")(functions.config().stripe.sk);

admin.initializeApp(functions.config().firebase);

const currency = functions.config().stripe.currency || 'EUR';
const PERCENTAGE_TAKEN_FROM_RIDES = 0.2;


/**
 * onCreation of a ride this function is triggered.
 * 
 * It will check if stripe has a customer card for the user that made the request and if so, or if,
 * the stripe is disabled (STRIPE_ENABLED=false) then it will create a geofire location in the db
 * which the drivers are listening to and, if they are within range, then they will be able to accept
 * the ride
 */
exports.newRequest = functions.database.ref('/ride_info/{pushId}').onCreate(async (snapshot, context) => {
    // Grab the current value of what was written to the Realtime Database.
    const original = snapshot.val();

    if(STRIPE_ENABLED){
        //check if user as a credit card enabled with stripe
        const customerPayment = await (await isCustomerPaymentReady(original.customerId)).success;
        console.log(customerPayment)
        if (!customerPayment) {
            await admin.database().ref().child(`ride_info/${context.params.pushId}`).update({ cancelled: true, cancelled_info: { type: 11, reason: 'No default payment method available' } });
            return Promise.reject(new Error("No payment method available"));
        }

    }
    await admin.database().ref().child(`ride_info/${context.params.pushId}`).update({ price_calculated: false });

    //Create geofire location with the pickup location of the request
    let geoFireRef = admin.database().ref().child('customer_requests');
    let geoFireApp = new geofire.GeoFire(geoFireRef);
    await geoFireApp.set(context.params.pushId, [original.pickup.lat, original.pickup.lng]);

    return Promise.resolve(200);
});

/**
 * This function triggers any time a ride gets updated
 * 
 * It will listen for cancel events or end events and, if a ride has ended, then calculate
 * the ride price
 */
exports.requestListener = functions.database.ref('/ride_info/{pushId}').onUpdate(async (snapshot, context) => {
    const id_customer = snapshot.after.val().customerId;
    const id_driver = snapshot.after.val().driverId;
    const id_ride = context.params.pushId;

    if (snapshot.after.val().ended || snapshot.after.val().cancelled) {
        let geoFireRef = admin.database().ref().child('customer_requests');
        let geoFireApp = new geofire.GeoFire(geoFireRef);
        await geoFireApp.remove(context.params.pushId);
    }
    if ((!snapshot.after.val().ended && !snapshot.after.val().cancelled) && Math.abs(snapshot.after.val().timestamp_last_driver_read - snapshot.after.val().creation_timestamp) / 1000 > REQUEST_TIMEOUT_SECONDS) {
        let geoFireRef = admin.database().ref().child('customer_requests');
        let geoFireApp = new geofire.GeoFire(geoFireRef);
        await geoFireApp.remove(context.params.pushId);
        await admin.database().ref().child(`ride_info/${id_ride}`).update({ cancelled: true, cancelled_info: { type: 10, reason: 'No driver found' } });

        return;
    }
    if (!snapshot.after.val().rating_calculated && snapshot.after.val().rating !== -1) {
        await admin.database().ref(`Users/Drivers/${id_driver}/rating/${id_ride}`).set(snapshot.after.val().rating);
        await admin.database().ref(`/ride_info/${id_ride}`).update({rating_calculated: true});
        
    }

    if (!snapshot.after.val().ended) { return; }
    if (snapshot.after.val().price_calculated) { return; }

    //Gets values of ride from the snap and initialize them
    var distance = Number(snapshot.after.val().distance);
    var price = Number(distance) * 0.5;
    //calculate duration of rides in minutes
    var duration = Number(((snapshot.after.val().timestamp - snapshot.after.val().timestamp_picked_customer) / 60000) % 60);

    //Calculates the final price for each ride price
    switch (snapshot.after.val().service) {
        case "type_1":
            price = 1 + distance * 1 + duration * 1;
            break;
        case "type_2":
            price = 2 + distance * 2 + duration * 2;
            break;
        case "type_3":
            price = 3 + distance * 3 + duration * 3;
            break;
        case "type_4":
            price = 4 + distance * 4 + duration * 4;
            break;
        case "type_5":
            price = 5 + distance * 5 + duration * 5;
            break;
    }

    //Updates the customer stripe with the new charge which will be called in another function
    await admin.database().ref(`stripe_customers/${id_customer}/charges/${id_ride}`).update({ amount: price })

    let currentPayout = await admin.database().ref(`Users/Drivers/${id_driver}/payout_amount/`).once('value');

    let payoutAmount = currentPayout.val();
    if (payoutAmount === undefined) {
        payoutAmount = 0;
    }

    const payoutFinal = payoutAmount + (price * PERCENTAGE_TAKEN_FROM_RIDES);

    //update the driver db with the new payout ammount
    await admin.database().ref(`Users/Drivers/${id_driver}`).update({ payout_amount: payoutFinal });
    await admin.database().ref(`/ride_info/${id_ride}`).update({price, price_calculated: true});

});


/**
 * check if user as a credit card enabled with stripe
 * @param {String} uid user id
 * @returns {boolean} true if customer does have a payment method enabled
 */
async function isCustomerPaymentReady(uid) {
    const snapshot = await admin.database().ref().child('stripe_customers').child(uid).once('value')
    const snapval = snapshot.val();
    const customer = snapval.customer_id


    await stripe.paymentMethods.list(
        { customer, type: 'card' }
    );

    const customerStripeInfo = await stripe.customers.retrieve(customer);

    if (customerStripeInfo.invoice_settings.default_payment_method === "" || customerStripeInfo.invoice_settings.default_payment_method === null) {
        return { success: false, error: "No default payment method selected" }
    }

    return { success: true }
}


/**
 * Checks for pending payouts and update them
 * @param {*} uid user id of the driver
 * @param {*} paymentId id of the payout
 */
function updatePayoutPending(uid, paymentId) {
    return admin.database().ref('ride_info/').orderByChild("driverId").equalTo(uid).once('value').then((snap) => {
        if (snap === null) {
            throw new Error("profile doesn't exist");
        }

        if (snap.hasChildren()) {
            snap.forEach(element => {
                if (element.val() === true) {
                    admin.database().ref('ride_info/' + element.key + 'payout').set({
                        driverPaidOut: true,
                        timestamp: admin.database.ServerValue.TIMESTAMP,
                        paymentId: paymentId
                    });
                }
            });
        }
        return null;
    }).catch((error) => {
        return console.error(error);
    });
}




/**
 * Creates a setup intent for a specific user
 * 
 * This will create a stripe user which will be used to
 */
exports.create_setup_intent = functions.https.onRequest(async (request, response) => {
    const snapshot = await admin.database().ref().child('stripe_customers').child(request.body.uid).once('value')
    const customer = snapshot.val().customer_id

    const setupIntent = await stripe.setupIntents.create({ customer });

    const clientSecret = setupIntent.client_secret

    // Send publishable key and SetupIntent details to client
    response.send({
        publishableKey: functions.config().stripe.pk,
        clientSecret: clientSecret
    });

});



/**
 * Charge the Stripe customer whenever an amount is written to the Realtime database
 * 
 * It's a trigger that is called whenever a charge is created
 */
exports.createStripeCharge = functions.database.ref('stripe_customers/{userId}/charges/{id}').onCreate(async (snap, context) => {
    const amount = Math.ceil(snap.val().amount);

    try {
        const snapshot = await admin.database().ref().child('stripe_customers').child(context.params.userId).once('value')
        const snapval = snapshot.val();
        const customer = snapval.customer_id
        // List the customer's payment methods to find one to charge
        const paymentMethods = await stripe.paymentMethods.list({
            customer,
            type: "card"
        });
        // Create and confirm a PaymentIntent with the order amount, currency, 
        // Customer and PaymentMethod ID
        await stripe.paymentIntents.create({
            amount: amount * 100,
            currency,
            payment_method: paymentMethods.data[0].id,
            customer,
            off_session: true,
            confirm: true
        });
    } catch (err) {
        console.log("Unknown error occurred", err);
    }
});

/**
 * When a user is created, register them with Stripe
 */
exports.createStripeCustomer = functions.auth.user().onCreate(async (user) => {
    const customer = await stripe.customers.create({ email: user.email });
    return admin.database().ref(`/stripe_customers/${user.uid}/`).set({ customer_id: customer.id });
});


/**
 * Changes the deafult card for a user
 * 
 * @param payment_id - id of the card to set to default
 */
exports.setDefaultCard = functions.https.onRequest(async (request, response) => {
    const snapshot = await admin.database().ref().child('stripe_customers').child(request.body.uid).once('value')
    const snapval = snapshot.val();
    const customer = snapval.customer_id

    await stripe.customers.update(
        customer,
        { invoice_settings: { default_payment_method: request.body.payment_id } }
    );

    response.send(200);
});

/**
 * removes a card of a user
 * 
 * @param payment_id - id of the card to set to remove
 */
exports.removeCard = functions.https.onRequest(async (request, response) => {
    await stripe.paymentMethods.detach(
        request.body.payment_id
    );
    response.send(200);
});

/**
 * Add a payment source (card) for a user by writing a stripe payment source token to Realtime database
 * 
 * It's a trigger that is called whenever a payment token is created
 */
exports.addPaymentSource = functions.database.ref('/stripe_customers/{userId}/tokens/{pushId}').onCreate(async (snap, context) => {
    const source = snap.data();
    const token = source.token;
    if (source === null) {
        return null;
    }
    try {
        const snapshot = await admin.database().ref(`stripe_customers`).equalTo(context.params.userId).once('value')
        const customer = snapshot.data().customer_id;
        const response = await stripe.customers.createSource(customer, { source: token });
        return admin.database().ref(`/stripe_customers/${user.uid}/sources/${response.fingerprint}`).set(response, { merge: true });

    } catch (error) {
        await snap.ref.set({ 'error': userFacingMessage(error) }, { merge: true });
        return reportError(error, { user: context.params.userId });
    }
});

/**
 * Returns a list of payment objects that the user with the uid in the query params has.
 * It does not return the full cards uncrypted, just the 4 last digits.
 */
exports.listCustomerCards = functions.https.onRequest(async (request, response) => {
    const snapshot = await admin.database().ref().child('stripe_customers').child(request.query.uid).once('value')
    const snapval = snapshot.val();
    const customer = snapval.customer_id


    const cards = await stripe.paymentMethods.list(
        { customer, type: 'card' }
    );

    const customerStripeInfo = await stripe.customers.retrieve(customer);

    let responseJson = { cards: cards.data, default_payment_method: customerStripeInfo.invoice_settings.default_payment_method }
    response.send(responseJson);
});


/**
 * When a driver sets up the account to be eligeble to payout this function is called
 * Saves the important info and notifies the db that this driver is able to receive
 * a payout
 */
exports.createStripeConnectAccount = functions.https.onRequest(async (request, response) => {
    let code = request.body.code;
    let state = request.body.state;

    const snapshot = await admin.database().ref(`Users/Drivers/`).orderByChild('connect_code').equalTo(state).once('value')
    let uid = Object.keys(snapshot.val())[0];

    var connect_account = await stripe.oauth.token({
        grant_type: 'authorization_code',
        code,
    });

    await admin.database().ref(`/stripe_customers/${uid}`).update({ connect_account });
    await admin.database().ref(`Users/Drivers/${uid}/connect_set`).set(true);

    response.send(connected_account_id)
});


/**
 * Starts a payout intent for a driver.
 * 
 * Make sure the stripe account has cash flow big enough to handle this requests.
 */

exports.payout = functions.https.onRequest(async (request, response) => {
    stripe.balance.retrieve(function (err, balance) {
        console.log({ balance: balance.available, err })
    });

    let payoutAmount = await admin.database().ref().child(`Users/Drivers/${request.body.uid}/payout_amount/`).once('value')
    payoutAmount = payoutAmount.val() * 100;
    const snapshot = await admin.database().ref().child(`stripe_customers/${request.body.uid}/connect_account/stripe_user_id`).once('value')
    const userConnectId = snapshot.val();

    let result = await stripe.transfers.create(
        {
            amount: Math.ceil(payoutAmount),
            currency: currency,
            destination: userConnectId,
        }
    );


    if (result === undefined) {
        return response.status('400').send();
    }

    await admin.database().ref(`stripe_customers/${request.body.uid}/payouts`).push();

    updatePayoutPending(request.body.uid, result.id).then(() => {
        return admin.database().ref('/Users/Drivers/' + request.body.uid + '/payout_amount/').set(0).then(() => {
            response.status('200').end();
            return;
        });
    }).catch((error) => {
        return console.error(error);
    })
    return null;

});


/**
 * Webhook that handles events that happen between stripe and this application
 */
exports.webhook = functions.https.onRequest(async (req, res) => {
    let data;
    let eventType;

    // Check if webhook signing is configured.
    if (process.env.STRIPE_WEBHOOK_SECRET) {
        // Retrieve the event by verifying the signature using the raw body and secret.
        let event;
        let signature = req.headers["stripe-signature"];

        try {
            event = await stripe.webhooks.constructEvent(
                req.rawBody,
                signature,
                process.env.STRIPE_WEBHOOK_SECRET
            );
        } catch (err) {
            console.log(`⚠️  Webhook signature verification failed.`);
            return res.sendStatus(400);
        }
        // Extract the object from the event.
        data = event.data;
        eventType = event.type;
    } else {
        // Webhook signing is recommended, but if the secret is not configured in `config.js`,
        // retrieve the event data directly from the request body.
        data = req.body.data;
        eventType = req.body.type;
    }

    if (eventType === "setup_intent.succeeded") {

        // Get Customer billing details from the PaymentMethod
        const paymentMethod = await stripe.paymentMethods.retrieve(
            data.object.payment_method
        );

        let dataRetrieved = await stripe.setupIntents.retrieve(data.object.id)

        // Create a Customer to store the PaymentMethod ID for later use
        await stripe.paymentMethods.attach(
            paymentMethod.id,
            { customer: dataRetrieved.customer }
        );

        await stripe.customers.update(
            dataRetrieved.customer,
            { invoice_settings: { default_payment_method: paymentMethod.id } }
        );

        // At this point, associate the ID of the Customer object with your
        // own internal representation of a customer, if you have one.

        // You can also attach a PaymentMethod to an existing Customer
        // https://stripe.com/docs/api/payment_methods/attach
    }

    res.sendStatus(200);
});