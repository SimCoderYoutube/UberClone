
'use strict';
const functions = require('firebase-functions');
const paypal = require('paypal-rest-sdk');
const admin = require('firebase-admin');
admin.initializeApp(functions.config().firebase);

paypal.configure({
    mode: 'sandbox',
    client_id: functions.config().paypal.client_id,
    client_secret: functions.config().paypal.client_secret
})


exports.newRequest = functions.database.ref('/history/{pushId}').onCreate(event => {
    var requestSnapshot = event.data;

    var distance  = requestSnapshot.child('distance').val();
    var price  = distance * 0.5;

    var pushId = event.params.pushId;

    return requestSnapshot.ref.parent.child(pushId).child('price').set(price);
    
});


function getPayoutsPending(uid){
    return admin.database().ref('Users/Drivers/' + uid + '/history').once('value').then((snap) =>{
        if(snap === null){
            throw new Error("profile doesn't exist");
        }
        var array = [];
        if(snap.hasChildren()){
            snap.forEach(element => {
                if(element.val() === true){
                    array.push(element.key);
                }
            });
        }
        return array;
    }).catch((error) => {
        return console.error(error);
    });
}


function getPayoutsAmount(array){
    return admin.database().ref('history').once('value').then((snap) =>{
        var value = 0.0;
        if(snap.hasChildren()){
            snap.forEach(element => {
                if(array.indexOf(element.key) > -1){
                        if(element.child('price').val() !== null){
                            value += element.child('price').val();
                        }
                }
            });
            return value;
        }
        return value;
    }).catch((error) => {
        return console.error(error);
    });
}



function updatePaymentsPending(uid, paymentId){
    return admin.database().ref('Users/Drivers/' + uid + '/history').once('value').then((snap) =>{
        if(snap === null){
            throw new Error("profile doesn't exist");
        }

        if(snap.hasChildren()){
            snap.forEach(element => {
                if(element.val() === true){
                    admin.database().ref('Users/Drivers/' + uid + '/history/' + element.key).set({
                        timestamp: admin.database.ServerValue.TIMESTAMP,
                        paymentId: paymentId
                    });
                    admin.database().ref('history/' + element.key + '/driverPaidOut').set(true);
                }
            });
        }
        return null;
    }).catch((error) => {
        return console.error(error);
    });
}







exports.payout = functions.https.onRequest((request, response) => {
    getPayoutsPending(request.body.uid).then((array) => {
        getPayoutsAmount(array).then((value) => {

            var valueTrunc = parseFloat(Math.round(value * 100) / 100).toFixed(2);

            const sender_batch_id = Math.random().toString(36).substring(9);
            const sync_mode = 'true';
            const payReq = JSON.stringify({
                sender_batch_header: {
                    sender_batch_id: sender_batch_id,
                    email_subject: "You have a payment"
                },
                items: [
                    {
                        recipient_type: "EMAIL",
                        amount: {
                            value: valueTrunc,
                            currency: "USD"
                        },
                        receiver: request.body.email,
                        note: "Thank you.",
                        sender_item_id: "item_3"
                    }
                ]
            });
            paypal.payout.create(payReq, sync_mode, (error, payout) => {
                if(error){
                    console.warn(error.response);
                    response.status('500').end();
                    throw error;
                }else{
                    console.info("payout created");
                    console.info(payout);
                    updatePaymentsPending(request.body.uid, sender_batch_id).then(() =>{
                        response.status('200').end();
                        return;
                    }).catch((error) => {
                        return console.error(error);
                    })
                }
            });



            return null;
        }).catch((error) => {
            return console.error(error);
        })

        return null;
    }).catch((error) => {
        return console.error(error);
    })
   
});


