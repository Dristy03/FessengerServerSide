package com.dristy.serverside;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.JsonReader;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackgroundIntentService extends JobIntentService {
    private static final String TAG = "ExJobIntentService";
    DocumentReference db;
    CollectionReference cr;
    boolean sent = false;
    boolean ok;
    int dd,mm,yy;
    NotificationManager notificationManager;
    private RequestQueue mRequestQueue;
    private  String URL ="https://fcm.googleapis.com/fcm/send";



    @Override
    public void onCreate() {
        super.onCreate();
      mRequestQueue = Volley.newRequestQueue(this);
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


    //checks whether all the mails of the day are sent or not
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

    //get all the mails from database of today to be sent
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
                    sendNotification(email.replace('@','_'));
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

    //sends notification
    private void sendNotification(String topic) {
        Log.d(TAG, "sendNotification: ");
        JSONObject notification = new JSONObject();
        JSONObject data = new JSONObject();
        try {
            data.put("title","Fessenger");
            data.put("body","Your message has just arrived.Check your mail");

            notification.put("to","/topics/" + topic); //topic have to be changed
            notification.put("data",data);
            notification.put("priority","high");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest request =new JsonObjectRequest(Request.Method.POST, URL
                , notification, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String>header = new HashMap<>();
                header.put("Content-Type","application/json");
                header.put("Authorization","key=" + "AAAAyjVGQBk:APA91bEt6Mj_HgqA3On1k5pPECZxZO1nxlXMBkaNSyn3_1WkDRS30bBciC5cu3SAa-4e3IJ-m46nSXXqZ7Vt0to_-fFP4AwDkF3kKQ9JmmVlQ-cdC3mPfBMVM-EFbiRghJwbAFyQxYyW");
                return header;
            }
        };
        mRequestQueue.add(request);

    }

    //sends mail to the user as desired
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
