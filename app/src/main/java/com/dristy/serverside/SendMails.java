package com.dristy.serverside;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMails extends AsyncTask<Void,Void,Void> {
    private Context context;
    private Session session;
    private String email;
    private String message;
    private String title;
   // private ProgressDialog progressDialog;

    public SendMails(Context context, String email, String title, String message) {
        this.context = context;
        this.email = email;
        this.message = message;
        this.title = title;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //progressDialog = ProgressDialog.show(context,"Sending message","Please wait....",false,false);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
       //
        //Toast.makeText(context,"Message Sent",Toast.LENGTH_LONG).show();
    }

    @Override
    protected Void doInBackground(Void... params) {
        Properties props = new Properties();

        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");


        session = Session.getDefaultInstance(props,
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("Fessenger003@gmail.com","!234567*");
                    }
                });

        try {

            MimeMessage mm = new MimeMessage(session);

            mm.setFrom(new InternetAddress("Fessenger003@gmail.com"));

            mm.addRecipient(Message.RecipientType.TO, new InternetAddress(email));

           mm.setSubject(title);
            mm.setText(message);

            Transport.send(mm);

        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;

    }
}

