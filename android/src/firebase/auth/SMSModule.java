package firebase.auth;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.telephony.TelephonyManager;

@Kroll.module(parentModule = TitaniumFirebaseAuthModule.class, propertyAccessors = {
		"onVerificationCompleted", "onError", "onCodeSent","onCodeAutoRetrievalTimeOut" })
public class SMSModule extends TitaniumFirebaseAuthModule {

	private PhoneAuthProvider.OnVerificationStateChangedCallbacks verifyPhoneNumberCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
		@Override
		public void onVerificationCompleted(PhoneAuthCredential credential) {
			signInWithPhoneAuthCredential(credential);
		}

		@Override
		public void onCodeSent(String verificationId,
				PhoneAuthProvider.ForceResendingToken token) {
			Log.d(LCAT, "onCodeSent");
			resendToken = token;
			KrollDict event = new KrollDict();
			event.put("verificationId", verificationId);
			event.put("resendToken", new ResendTokenProxy(token));
			event.put("success", true);
			if (onCodeSentCallback != null)
				onCodeSentCallback.callAsync(getKrollObject(), event);
		}

		@Override
		public void onCodeAutoRetrievalTimeOut(String verificationId) {
			KrollDict event = new KrollDict();
			event.put("verificationId", verificationId);
			if (onCodeAutoRetrievalTimeOutCallback != null)
				onCodeAutoRetrievalTimeOutCallback.callAsync(getKrollObject(), event);
		}

		@Override
		public void onVerificationFailed(FirebaseException e) {
			KrollDict event = new KrollDict();
			event.put("error", true);
			if (e instanceof FirebaseAuthInvalidCredentialsException) {
				event.put("message", "Invalid request");
				event.put("code", ERROR_INVALID_REQUEST);
			} else if (e instanceof FirebaseTooManyRequestsException) {
				event.put("message",
						"The SMS quota for the project has been exceeded");
				event.put("code", ERROR_QUOTA_EXCEEDED);
			} else {
				event.put("message", e.getMessage());
				event.put("code", ERROR_INTERNAL);
				e.printStackTrace();
			}
			if (onErrorCallback != null)
				onErrorCallback.callAsync(getKrollObject(), event);
		}
	};
	@Kroll.constant
	public static final int ERROR_INTERNAL = 0;
	@Kroll.constant
	public static final int ERROR_INVALID_REQUEST = 1;
	@Kroll.constant
	public static final int ERROR_QUOTA_EXCEEDED = 2;
	
	@Kroll.constant
	public static final int ERROR_INVALID_CREDENTIALS = 4;

	@Kroll.constant
	public static final String LCAT = "☎ ️" + TitaniumFirebaseAuthModule.LCAT;
	private KrollFunction onErrorCallback;
	private KrollFunction onVerificationCompletedCallback;
	private KrollFunction onCodeSentCallback;
	private KrollFunction onCodeAutoRetrievalTimeOutCallback;
	private PhoneAuthProvider.ForceResendingToken resendToken;
	private String phoneNumber = "";
	private int timeout = 30000;
	private FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
	private PhoneAuthProvider phoneAuthProvider = PhoneAuthProvider
			.getInstance(firebaseAuth);

	public SMSModule() {
		super();
	}

	@Kroll.method
	public SMSModule setTimeout(int timeout) {
		if (timeout > 20000 && timeout > 60000)
			this.timeout = timeout;
		registerCallbacks();
		return this;
	}

	/* Start verifying by phone number SMS */
	@Kroll.method
	public SMSModule validatePhoneNumber(String phoneNumber,
			@Kroll.argument(optional = true) Object callback) {
		if (callback != null && callback instanceof KrollFunction)
			onCodeSentCallback = (KrollFunction) callback;
		this.phoneNumber = phoneNumber;
		registerCallbacks();
		doVerifyPhoneNumber();
		return this;
	}

	private void registerCallbacks() {
		if (hasProperty("onVerificationCompleted")) {
			Object o = getProperty("onVerificationCompleted");
			if (o instanceof KrollFunction) {
				onVerificationCompletedCallback = (KrollFunction) o;
			}
		}
		if (hasProperty("onError")) {
			Object o = getProperty("onError");
			if (o instanceof KrollFunction) {
				onErrorCallback = (KrollFunction) o;
			}
		}
		if (hasProperty("onCodeSent")) {
			Object o = getProperty("onCodeSent");
			if (o instanceof KrollFunction) {
				onCodeSentCallback = (KrollFunction) o;
			}
		}
		if (hasProperty("onCodeAutoRetrievalTimeOut")) {
			Object o = getProperty("onCodeAutoRetrievalTimeOut");
			if (o instanceof KrollFunction) {
				onCodeAutoRetrievalTimeOutCallback = (KrollFunction) o;
			}
		}
		
	}

	private void doVerifyPhoneNumber() {
		Log.d(LCAT, "PROVIDER_ID=" + PhoneAuthProvider.PROVIDER_ID);
		if (phoneAuthProvider != null) {
			phoneAuthProvider.verifyPhoneNumber(//
					phoneNumber, //
					timeout, //
					TimeUnit.MILLISECONDS,//
					TiApplication.getInstance().getCurrentActivity(), //
					verifyPhoneNumberCallbacks);
		} else
			Log.d(LCAT, "phoneAuthProvider is null");
	}

	private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
		Activity activity = TiApplication.getInstance().getCurrentActivity();
		FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
		firebaseAuth.signInWithCredential(credential).addOnCompleteListener(
				activity, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						KrollDict event = new KrollDict();
						if (task.isSuccessful()) {
							dispatchSuccess(task.getResult().getUser());
						} else {
							Log.w(LCAT, "signInWithCredential:failure",
									task.getException());
							if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
								if (onErrorCallback != null) {
									event.put("success", false);
									event.put("message", task.getException()
											.getMessage());
								}
							}
						}
					}
				});
	}

	@Kroll.method
	public void signInWithSMSCode(String a, String b) {
		String verificationId;
		String code;
		if (a.length() > b.length()) {
			verificationId = a;
			code = b;
		} else {
			verificationId = b;
			code = a;
		}
		Log.d(LCAT, "code:id=" + code + ":" + verificationId);
		PhoneAuthCredential credential = PhoneAuthProvider.getCredential(
				verificationId, code);
		Activity activity = TiApplication.getAppCurrentActivity();
		firebaseAuth.signInWithCredential(credential).addOnCompleteListener(
				activity, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d(LCAT, "onComplete after signInWithSMSCode");
						if (task.isSuccessful()) {
							dispatchSuccess(task.getResult().getUser());
						} else {
							if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
								KrollDict event = new KrollDict();
								if (onErrorCallback != null) {
									event.put("success", false);
									event.put("code", ERROR_INVALID_CREDENTIALS);

									event.put("message", task.getException()
											.getMessage());
								}
							}
						}
					}
				});
	};

	@Kroll.method
	public void resendVerificationCode(String phoneNumber) {
		PhoneAuthProvider.getInstance().verifyPhoneNumber(phoneNumber,
				60, // Timeout
				TimeUnit.SECONDS, // Unit of timeout
				TiApplication.getInstance().getCurrentActivity(),
				verifyPhoneNumberCallbacks, // OnVerificationStateChangedCallbacks
				resendToken); // ForceResendingToken from callbacks
	}

	private void dispatchSuccess(FirebaseUser user) {
		KrollDict event = new KrollDict();

		event.put("success", true);
		event.put("user", UserModule.dictionaryFromUser(user)); // converts
																// to
																// KrollDict
		if (onVerificationCompletedCallback != null)
			onVerificationCompletedCallback.callAsync(getKrollObject(), event);
		else
			Log.w(LCAT,
					"no callback for succes available, you need to define a property `onVerificationCompleted`");
	}
}
