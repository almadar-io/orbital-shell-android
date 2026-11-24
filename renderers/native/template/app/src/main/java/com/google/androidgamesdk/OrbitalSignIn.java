package com.google.androidgamesdk;

import android.app.Activity;
import android.os.CancellationSignal;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import java.util.concurrent.Executors;

/**
 * The native-gpu host's sign-in (owner 2026-11-23): Google through Credential Manager, then Firebase Auth, so the
 * ID token the server verifies is the one the web host sends. The Rust host asks with {@link #signIn} and
 * {@link #signOut}; every outcome and every token refresh comes back through the native callbacks. The token goes to
 * the transport's bearer slot and never to the program.
 */
public final class OrbitalSignIn {
    private static Activity activity;

    private OrbitalSignIn() {}

    /** Whether this build carries a Firebase config (google-services.json); an app without one has no sign-in. */
    private static boolean configured(Activity host) {
        return !FirebaseApp.getApps(host).isEmpty();
    }

    public static void install(Activity host) {
        activity = host;
        nativeInstall();
        if (!configured(host)) {
            return;
        }
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.addIdTokenListener((FirebaseAuth.IdTokenListener) changed -> sendToken(changed.getCurrentUser()));
    }

    private static void sendToken(FirebaseUser user) {
        if (user == null) {
            nativeToken(null);
            return;
        }
        user.getIdToken(false).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                nativeToken(task.getResult().getToken());
            }
        });
    }

    private static String webClientId(Activity host) {
        int id = host.getResources().getIdentifier("default_web_client_id", "string", host.getPackageName());
        return id == 0 ? "" : host.getString(id);
    }

    public static void signIn() {
        Activity host = activity;
        if (host == null) {
            nativeSignInFailed("unavailable", "sign-in is not installed");
            return;
        }
        if (!configured(host)) {
            nativeSignInFailed("unavailable", "this build has no google-services.json");
            return;
        }
        String clientId = webClientId(host);
        if (clientId.isEmpty()) {
            nativeSignInFailed("unavailable", "this build has no google-services.json (default_web_client_id)");
            return;
        }
        GetGoogleIdOption option = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(option).build();
        CredentialManager.create(host).getCredentialAsync(host, request, new CancellationSignal(), Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (!(credential instanceof CustomCredential)
                                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            nativeSignInFailed("invalid", "the credential is not a Google ID token");
                            return;
                        }
                        String googleToken = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData()).getIdToken();
                        FirebaseAuth.getInstance()
                                .signInWithCredential(GoogleAuthProvider.getCredential(googleToken, null))
                                .addOnCompleteListener(signedIn -> {
                                    FirebaseUser user = signedIn.isSuccessful() ? FirebaseAuth.getInstance().getCurrentUser() : null;
                                    if (user == null) {
                                        Exception error = signedIn.getException();
                                        nativeSignInFailed("denied", error == null ? "Firebase refused the sign-in" : error.getMessage());
                                        return;
                                    }
                                    user.getIdToken(false).addOnCompleteListener(token -> {
                                        if (!token.isSuccessful() || token.getResult() == null) {
                                            nativeSignInFailed("unavailable", "no ID token for the signed-in user");
                                            return;
                                        }
                                        nativeSignedIn(user.getUid(), user.getEmail(), user.getDisplayName(), token.getResult().getToken());
                                    });
                                });
                    }

                    @Override
                    public void onError(GetCredentialException error) {
                        String kind = error instanceof GetCredentialCancellationException ? "cancelled" : "unavailable";
                        nativeSignInFailed(kind, error.getMessage() == null ? error.getType() : error.getMessage());
                    }
                });
    }

    public static void signOut() {
        Activity host = activity;
        if (host != null && configured(host)) {
            FirebaseAuth.getInstance().signOut();
        }
        nativeSignedOut();
    }

    private static native void nativeInstall();

    private static native void nativeToken(String idToken);

    private static native void nativeSignedIn(String uid, String email, String displayName, String idToken);

    private static native void nativeSignInFailed(String kind, String message);

    private static native void nativeSignedOut();
}
