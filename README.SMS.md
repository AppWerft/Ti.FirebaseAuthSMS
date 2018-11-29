# Firebase Auth.SMS - Titanium Module
Use the native Firebase SDK in Axway Titanium. This repository is part of the [Titanium Firebase](https://github.com/hansemannn/titanium-firebase) project. This fork addes the SMS functionality. It is currently only available for Android.


## Methods

### setTimeout(int millisecond)
With this optional method you can modify the standard timeout of 30 sec. 

It returns the module self, so you can concatenate:


```javascript
Firebase.Auth.SMS.setTimeout(20000).validatePhoneNumber("+494060812460");
```

### validatePhoneNumber(String phoneNumber, Function callback);

- phoneNumber: number to test, must start with '+'
- callback: this is mainly for internal `onCodeSent` event. It will called after sending of SMS for starting an UI to ask the user for code. The payload consist of an object with a couple of properties: 
- success (boolean)
- verificationId (you needed for second call)
- optional resendToken

Optional you can ommit this second optional parameter and you can use a property method `onCodeSent`:

```javascript
Firebase.Auth.SMS.onCodeSent = function(e) {
}
```

### verifyPhoneNumberWithCode(String verificationId, String SMScode);


## Callbacks

### onVerificationCompleted

This is a module based callback for receiving messages from SDK.

#### Usage

```javascript
FirebaseAuth.SMS.onVerificationCompleted = function(e) {
	console.log(e.success);
	console.log(e.user);
}
```
#### return value
is an object with properties:
- success
- userObject

In case of succesful login a  token will saved to app and you can access to the user object evertime you start the app.
### onCodeSent
This optional callback has the same functionality like the second optional paramter of `validatePhoneNumber`

### onError

All errors will dispatched here. All error payloads contains a (string) message from system and a code number:

##### ERROR\_INVALID\_REQUEST
The Firebase api sends the text "Invalid request"
 
##### ERROR\_QUOTA\_EXCEEDED
The SMS quota for the project has been exceeded


##### ERROR\_INVALID\_CREDENTIALS
The sms code is wrong

##### ERROR\_INTERNAL
This eror comes directly from api without special exception, please read the textual message

In case of last error a stacktrace will printed.  


### onCodeAutoRetrievalTimeOut
This event will fired if this event will fired by api. It sends back the `verificationId`.

## Deleting of user
This function is not part of this module, but you can logout the user by usage:

```javascript
FirebaseAuth.User.deleteUser(function(){});

```
Attention: this function is only available in this patched version of firebase.auth. PR is requested but still not done. 

Important: To delete a user, the user must have signed in recently. See [Re-authenticate a user](https://firebase.google.com/docs/auth/android/manage-users#re-authenticate_a_user). For this you have to persist `verificationId` and `smscode` in your app (i.e. in properties)


```javascript
const verificationId = Ti.App.Properties.getString("FIREBASE_VERFICATIONID","");
const smsCode = Ti.App.Properties.getString("FIREBASE_SMSCODE","");
FirebaseAuth.User.reauthenticate(verificationId, smscode, function(e){
	if (e.success) 
		FirebaseAuth.User.deleteUser();
});

```
## User object

* email
* phoneNumber
* providerID (phone | password | google | facebook | github | twitter | oauth)
* uid
* photoURL
* displayName
* isEmailVerified
* isAnonymous

## Usage patterns

### Width second callback parameter
```javascript
Firebase.SMS.onError = function(e) {
	console.log(e);
}; 
Firebase.SMS.onVerificationCompleted = function(e) {
	console.log(e.user);
}
Firebase.Auth.SMS.validatePhoneNumber(NUMBER,function(e){
	// UI for SMScode Input
	verifyPhoneNumberWithCode(verificationId, SMScode);
});
```

### With callback property

 ```javascript
Firebase.SMS.onError = function(e) {
	console.log(e);
}; 
Firebase.SMS.onVerificationCompleted = function(e) {
	console.log(e.user);
};
Firebase.SMS.onCodeSent = function(e){
	// UI for SMScode Input
	verifyPhoneNumberWithCode(e.verificationId, SMScode);
};

Firebase.Auth.SMS.validatePhoneNumber(NUMBER);
