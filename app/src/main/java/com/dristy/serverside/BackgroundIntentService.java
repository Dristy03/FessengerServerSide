package com.dristy.serverside;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class BackgroundIntentService extends JobIntentService {
    private static final String TAG = "ExJobIntentService";
    DocumentReference db;
    CollectionReference cr;
    boolean sent = false;
    boolean ok;
    int dd,mm,yy;
    NotificationManager notificationManager;
    @Override
    public void onCreate() {
        super.onCreate();

        ok = true;
        Log.d(TAG, "onCreate: ");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,"1")
                .setContentTitle("Fessenger")
                .setContentText("Backend service running")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setColor(ContextCompat.getColor(this, R.color.colorAccent))
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(2)
                .setShowWhen(true);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel("1","name",NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(notificationChannel);
        }
        notificationManager.notify(1,builder.build());


        Calendar calendar= Calendar.getInstance();

        yy = calendar.get(Calendar.YEAR);
        mm = calendar.get(Calendar.MONTH) + 1;
        dd = calendar.get(Calendar.DATE);
        String mmS = ((mm<10)?"0"+mm:mm+"");
        String ddS = ((dd<10)?"0"+dd:dd+"");
        Log.d(TAG, "onCreate: "+ mmS);
        Log.d(TAG, "onCreate: "+ ddS);
        db =FirebaseFirestore.getInstance().collection("MailDatabase").document(calendar.get(Calendar.YEAR)+"")
                .collection(mmS+"").document(ddS+"");
        cr = FirebaseFirestore.getInstance().collection("MailDatabase").document(calendar.get(Calendar.YEAR)+"")
                .collection(mmS+"").document(ddS+"").collection("Emails");

        FirebaseAuth.getInstance().signInWithEmailAndPassword("test5@gmail.com","123456").addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    Log.d(TAG, "onComplete: ok ");
                }
            }
        });
    }

    static void enqueueWork(Context context, Intent work){
        enqueueWork(context,BackgroundIntentService.class,123,work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {


        while(ok){

            checksent();

            SystemClock.sleep(1000*60*7);
        }
    }

    private void checksent() {
        db.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful() && task.getResult()!=null && task.getResult().exists() && task.getResult().getData()!=null ){
                    Log.d(TAG, "onComplete: true " );
                    if( !task.getResult().getBoolean("SentAllMails")){
                        sendMails();
                    }
                }
                else Log.d(TAG, "onComplete: " + task.getException());
            }
        });
    }
    void sendMails(){

        cr.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<DocumentSnapshot> snap = queryDocumentSnapshots.getDocuments();
                for(DocumentSnapshot snapshot: snap){
                    String email = snapshot.getString("Email");
                    String title = snapshot.getString("Title");
                    String message = snapshot.getString("Message");
                    sendEmails(email,title,message);
                }
                HashMap<String, Boolean>map = new HashMap<>();
                map.put("SentAllMails",true);
                db.set(map);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, e.getLocalizedMessage());
            }
        });
        Log.d(TAG, "sendMails: " + "done");

    }

    private void sendEmails(String email, String title, String message) {
        Log.d(TAG, "onSendMail: " + email + " " + message );
        SendMails sendMail=new SendMails(this,email,title,message);
        sendMail.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ok=false;
        Log.d(TAG, "onDestroy: ");
    }

    @Override
    public boolean onStopCurrentWork() {
        Log.d(TAG, "onStopCurrentWork: ");
        notificationManager.cancelAll();
        return super.onStopCurrentWork();
    }
}
