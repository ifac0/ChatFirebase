package br.com.chatfirebase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;
import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.Item;
import com.xwray.groupie.ViewHolder;

import java.util.List;

import javax.annotation.Nullable;

public class ChatActivity extends AppCompatActivity {

    private GroupAdapter adapter;
    private User user;
    private Contact contact;
    private User me;
    private Contact mec;

    private EditText editChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if(getIntent().getExtras().getParcelable("user") != null){

            user = getIntent().getExtras().getParcelable("user");
            getSupportActionBar().setTitle(user.getUsername());

            getSupportActionBar().setTitle(user.getUsername());


        }else if(getIntent().getExtras().getParcelable("contact") != null){

            contact = getIntent().getExtras().getParcelable("contact");
            getSupportActionBar().setTitle(contact.getUsername());

            getSupportActionBar().setTitle(contact.getUsername());


        }


        RecyclerView rv = findViewById(R.id.recyclerChat);
        editChat = findViewById(R.id.editChat);
        Button btnChat = findViewById(R.id.btnChat);
        btnChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });

        adapter = new GroupAdapter();
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        if(getIntent().getExtras().getParcelable("user") != null) {

            FirebaseFirestore.getInstance().collection("/users")
                    .document(FirebaseAuth.getInstance().getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            me = documentSnapshot.toObject(User.class);
                            fetchMessages();
                        }
                    });

        }else if(getIntent().getExtras().getParcelable("contact") != null){

            FirebaseFirestore.getInstance().collection("/users")
                    .document(FirebaseAuth.getInstance().getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            mec = documentSnapshot.toObject(Contact.class);
                            fetchMessages();
                        }
                    });
        }
    }

    private void fetchMessages() {
        if(me != null){

            String fromId = me.getUuid();
            String toId = user.getUuid();

            FirebaseFirestore.getInstance().collection("/conversations")
                    .document(fromId)
                    .collection(toId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentChange> documentChanges = queryDocumentSnapshots.getDocumentChanges();
                            
                            if(documentChanges != null){
                                for (DocumentChange doc: documentChanges){
                                    if(doc.getType() == DocumentChange.Type.ADDED){
                                        Message message = doc.getDocument().toObject(Message.class);
                                        adapter.add(new MessageItem(message));
                                    }
                                }
                            }
                        }
                    });

        }else if(mec != null){

            String fromId = mec.getUuid();
            String toId = contact.getUuid();

            FirebaseFirestore.getInstance().collection("/conversations")
                    .document(fromId)
                    .collection(toId)
                    .orderBy("timestamp", Query.Direction.ASCENDING)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots, @Nullable FirebaseFirestoreException e) {
                            List<DocumentChange> documentChanges = queryDocumentSnapshots.getDocumentChanges();

                            if(documentChanges != null){
                                for (DocumentChange doc: documentChanges){
                                    if(doc.getType() == DocumentChange.Type.ADDED){
                                        Message message = doc.getDocument().toObject(Message.class);
                                        adapter.add(new MessageItem(message));
                                    }
                                }
                            }
                        }
                    });
        }
    }

    private void sendMessage() {
        String text = editChat.getText().toString();

        editChat.setText(null);

        final String fromId = FirebaseAuth.getInstance().getUid();

        if (user != null) {
            final String toId = user.getUuid();
            long timestamp = System.currentTimeMillis();

            final Message message = new Message();
            message.setFromId(fromId);
            message.setToId(toId);
            message.setTimestamp(timestamp);
            message.setText(text);

            if (!message.getText().isEmpty()) {
                FirebaseFirestore.getInstance().collection("/conversations")
                        .document(fromId)
                        .collection(toId)
                        .add(message)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Erro", documentReference.getId());

                                Contact contact = new Contact();
                                contact.setUuid(toId);
                                contact.setUsername(user.getUsername());
                                contact.setPhotoUrl(user.getProfileUrl());
                                contact.setTimestamp(message.getTimestamp());
                                contact.setLastMessage(message.getText());


                                FirebaseFirestore.getInstance().collection("/last-messages")
                                        .document(fromId)
                                        .collection("contacts")
                                        .document(toId)
                                        .set(contact);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Erro", e.getMessage(), e);
                            }
                        });

                FirebaseFirestore.getInstance().collection("/conversations")
                        .document(toId)
                        .collection(fromId)
                        .add(message)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Erro", documentReference.getId());

                                Contact contact = new Contact();
                                contact.setUuid(toId);
                                contact.setUsername(user.getUsername());
                                contact.setPhotoUrl(user.getProfileUrl());
                                contact.setTimestamp(message.getTimestamp());
                                contact.setLastMessage(message.getText());

                                FirebaseFirestore.getInstance().collection("/last-messages")
                                        .document(toId)
                                        .collection("contacts")
                                        .document(fromId)
                                        .set(contact);

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Erro", e.getMessage(), e);
                            }
                        });


            }
        }else if(contact != null){

            final String toId = contact.getUuid();
            long timestamp = System.currentTimeMillis();

            final Message message = new Message();
            message.setFromId(fromId);
            message.setToId(toId);
            message.setTimestamp(timestamp);
            message.setText(text);

            if (!message.getText().isEmpty()) {
                FirebaseFirestore.getInstance().collection("/conversations")
                        .document(fromId)
                        .collection(toId)
                        .add(message)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Erro", documentReference.getId());

                                Contact contact = new Contact();
                                contact.setUuid(toId);
                                contact.setUsername(contact.getUsername());
                                contact.setPhotoUrl(contact.getPhotoUrl());
                                contact.setTimestamp(message.getTimestamp());
                                contact.setLastMessage(message.getText());


                                FirebaseFirestore.getInstance().collection("/last-messages")
                                        .document(fromId)
                                        .collection("contacts")
                                        .document(toId)
                                        .set(contact);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Erro", e.getMessage(), e);
                            }
                        });

                FirebaseFirestore.getInstance().collection("/conversations")
                        .document(toId)
                        .collection(fromId)
                        .add(message)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d("Erro", documentReference.getId());

                                Contact contact = new Contact();
                                contact.setUuid(toId);
                                contact.setUsername(contact.getUsername());
                                contact.setPhotoUrl(contact.getPhotoUrl());
                                contact.setTimestamp(message.getTimestamp());
                                contact.setLastMessage(message.getText());

                                FirebaseFirestore.getInstance().collection("/last-messages")
                                        .document(toId)
                                        .collection("contacts")
                                        .document(fromId)
                                        .set(contact);

                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e("Erro", e.getMessage(), e);
                            }
                        });


            }

        }
    }

    private class MessageItem extends Item<ViewHolder>{

        private final Message message;

        private MessageItem(Message message) {
            this.message = message;
        }

        @Override
        public void bind(@NonNull ViewHolder viewHolder, int position) {
            TextView txtMsg = viewHolder.itemView.findViewById(R.id.txtMessage);
            ImageView imgMessage = viewHolder.itemView.findViewById(R.id.imageMessageUser);

            txtMsg.setText(message.getText());

            if(user != null) {
                Picasso.get()
                        .load(user.getProfileUrl())
                        .into(imgMessage);
            }else if(contact != null) {
                Picasso.get()
                        .load(contact.getPhotoUrl())
                        .into(imgMessage);
            }
        }

        @Override
        public int getLayout() {
            return message.getFromId().equals(FirebaseAuth.getInstance().getUid())
                    ? R.layout.item_to_message : R.layout.item_from_message;
        }
    }
}
