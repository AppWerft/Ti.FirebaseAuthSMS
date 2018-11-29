const FirebaseCore = require('firebase.core');

FirebaseCore.configure('firebase.json');

const FirebaseAuth = require('firebase.auth');

var $ = Ti.UI.createWindow({
	title : '(SMS-)Auth-Test',
	layout : 'vertical'
});

$.add(Ti.UI.createTextField({
	value : Ti.App.Properties.getString("N", ""),
	hintText : "Your phoneNumber with leading +44……",
	height : 50,
	inputType : Ti.UI.INPUT_TYPE_CLASS_NUMBER,
	keyboardType : Titanium.UI.KEYBOARD_TYPE_PHONE_PAD,
	top : 5,
	width : '80%'
}));
$.add(Ti.UI.createButton({
	height : 50,
	top : 5,
	title : "Verify number!",
	width : '80%'
}));
const inputField = $.children[0];
const Button = $.children[1];

$.addEventListener('open', function() {
	FirebaseCore.deleteInstanceId(function(e) {
		if (e.success) {
			verifyPhoneNumber();

		}
	});
});

////////////////////////////////////////////////////////////
function verifyPhoneNumber() {
	var verificationId = "";
	function onFirstClick() {
		Button.removeEventListener('click', onFirstClick);
		const number = inputField.getValue();
		Ti.App.Properties.setString("N", number);
		FirebaseAuth.SMS.verifyPhoneNumber({
			phoneNumber : number,
			timeout : 30000,
			oncodesent : function(e) {
				console.log(e);
				verificationId = e.verificationId;
				inputField.setValue('');
				inputField.hintText = 'Your SMS code …';
				inputField.focus();
				Button.setTitle('Test SMS code');
				Button.addEventListener('click', onSecondClick);
			},
			onverificationcompleted : function(e) {
				console.log(e);
				FirebaseAuth.User.updateProfile({
					displayName : "Mustermann",
					photoUrl : "https://avatars0.githubusercontent.com/u/2996237?s=460&v=4",
					email : "ras@heise.de",
					password : "88888888",
					callback : function(e) {
						console.log(e);
					}
				});
			},
			onerror : function(e) {
				alert(e.message);
			}
		});
	}

	function onSecondClick(e) {
		console.log("verificationId = " + verificationId);
		FirebaseAuth.SMS.verifyPhoneNumberWithCode(verificationId, inputField.getValue());
	}


	Button.addEventListener('click', onFirstClick);
}

$.open();
