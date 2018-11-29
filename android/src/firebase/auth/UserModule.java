package firebase.auth;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.titanium.TiApplication;

import android.app.Activity;
import android.net.Uri;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.auth.AuthCredential;

@Kroll.module(parentModule = TitaniumFirebaseAuthModule.class)
public class UserModule extends TitaniumFirebaseAuthModule {

	private KrollFunction onChangedCallback;

	public UserModule() {
		super();
	}

	@Kroll.onAppCreate
	public static void onAppCreate(TiApplication app) {

	}

	@Kroll.method
	public void deleteUser(@Kroll.argument(optional = true) final KrollFunction callback) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null)
			return;
		user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				KrollDict event = new KrollDict();
				if (task.isSuccessful())
					event.put("success", true);
				else
					event.put("success", false);
				if (callback != null) {
					callback.call(getKrollObject(), event);
				}
			}
		});
	}

	@Kroll.method
	public void reauthenticate(String foo, String bar,final @Kroll.argument(optional = true) KrollFunction callback) {
		AuthCredential credential = null;
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null)
			return;

		switch (user.getProviderId()) {
		case "phone":
			credential = PhoneAuthProvider.getCredential(foo, bar);

			break;
		case "email":
			credential = EmailAuthProvider.getCredential(foo, bar);
			break;
		}
		;
		if (credential == null)
			return;
		user.reauthenticate(credential).addOnCompleteListener(
				new OnCompleteListener<Void>() {
					@Override
					public void onComplete(@NonNull Task<Void> task) {

					}
				});

	}

	@Kroll.method
	public void verifyEmail(KrollDict opts) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		Activity activity = TiApplication.getInstance().getCurrentActivity();
		user.sendEmailVerification()
				.addOnCompleteListener(activity,
						new OnCompleteListener<Void>() {
							@Override
							public void onComplete(@NonNull Task<Void> task) {
								if (task.isSuccessful()) {
									KrollDict event = new KrollDict();
									event.put("success", true);
									event.put("type", "phoneNumber");
									sendBack(event);
								}
							}
						}).addOnFailureListener(new OnFailureListener() {
					@Override
					public void onFailure(@NonNull Exception e) {
						KrollDict event = new KrollDict();
						event.put("success", false);
						event.put("type", "phoneNumber");
						event.put("message", e.getMessage());
						sendBack(event);

					}
				}

				);
	};

	@Kroll.method
	public void updateProfile(KrollDict opts) {
		String displayName = null;
		String photoUrl = null;
		String email = null;
		String password = null;
		String phoneNumber = null;

		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user == null)
			return;

		if (opts.containsKeyAndNotNull("displayName")) {
			displayName = opts.getString("displayName");
		}
		if (opts.containsKeyAndNotNull("phoneNumber")) {
			phoneNumber = opts.getString("phoneNumber");
		}
		if (opts.containsKeyAndNotNull("photoUrl")) {
			photoUrl = opts.getString("photoUrl");
		}
		if (opts.containsKeyAndNotNull("email")) {
			email = opts.getString("email");
		}
		if (opts.containsKeyAndNotNull("password")) {
			password = opts.getString("password");
		}
		if (opts.containsKeyAndNotNull("callback")) {
			onChangedCallback = (KrollFunction) opts.get("callback");
		}
		if (opts.containsKeyAndNotNull("onchanged")) {
			onChangedCallback = (KrollFunction) opts.get("onchanged");
		}
		if (displayName != null || photoUrl != null) {
			UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
			if (displayName != null)
				builder.setDisplayName(displayName);
			if (photoUrl != null)
				builder.setPhotoUri(Uri.parse(photoUrl));
			UserProfileChangeRequest profileUpdates = builder.build();
			user.updateProfile(profileUpdates)
					.addOnCompleteListener(new OnCompleteListener<Void>() {
						@Override
						public void onComplete(@NonNull Task<Void> task) {
							if (task.isSuccessful()) {
								KrollDict event = new KrollDict();
								event.put("type", "profile");
								event.put("success", true);
								sendBack(event);
							}
						}
					}).addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception e) {

							KrollDict event = new KrollDict();
							event.put("success", false);
							event.put("type", "profile");
							event.put("message", e.getMessage());
							sendBack(event);

						}
					});

		}
		if (phoneNumber != null) {
			// TODO
		}
		if (email != null) {
			user.updateEmail(email)
					.addOnCompleteListener(new OnCompleteListener<Void>() {
						@Override
						public void onComplete(@NonNull Task<Void> task) {
							if (task.isSuccessful()) {
								KrollDict event = new KrollDict();
								event.put("success", true);
								event.put("type", "email");
								sendBack(event);
							}
						}
					}).addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception e) {
							KrollDict event = new KrollDict();
							event.put("success", false);
							event.put("type", "email");
							event.put("message", e.getMessage());

							sendBack(event);

						}
					});
		}
		if (password != null) {
			user.updateEmail(password)
					.addOnCompleteListener(new OnCompleteListener<Void>() {
						@Override
						public void onComplete(@NonNull Task<Void> task) {
							if (task.isSuccessful()) {
								KrollDict event = new KrollDict();
								event.put("type", "password");
								event.put("success", true);
								event.put("type", "password");

								sendBack(event);
							}
						}
					}).addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception e) {
							KrollDict event = new KrollDict();
							event.put("type", "password");
							event.put("success", false);
							event.put("message", e.getMessage());
							sendBack(event);

						}
					});
		}
	}

	@Kroll.method
	public static KrollDict dictionaryFromUser(FirebaseUser user) {
		if (user == null)
			return null;
		KrollDict result = new KrollDict();
		result.put("email", user.getEmail());
		result.put("phoneNumber", user.getPhoneNumber());
		result.put("providerID", user.getProviderId());
		result.put("uid", user.getUid());
		Uri photoURL = user.getPhotoUrl();
		if (photoURL != null) {
			result.put("photoURL", photoURL.toString());
		}
		result.put("displayName", user.getDisplayName());
		result.put("isEmailVerified", user.isEmailVerified());
		result.put("isAnonymous", user.isAnonymous());

		return result;
	}

	@Kroll.method
	public KrollDict getUser() {
		return null;
	}

	private void sendBack(KrollDict event) {
		FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
		if (user != null)
			event.put("user", dictionaryFromUser(user));
		KrollFunction onChanged = (KrollFunction) getProperty("onChanged");
		if (onChanged != null) {
			onChanged.call(getKrollObject(), new Object[] { event });
		}
		if (onChangedCallback != null) {
			onChangedCallback.call(getKrollObject(), new Object[] { event });
		}
		// if (this.hasListeners("changed") && user!=null) {
		// this.fireEvent("changed", event);
		// }
	}
}
