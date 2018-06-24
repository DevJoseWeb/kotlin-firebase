package com.javasampleapproach.kotlin.firebase.realtimedb

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_message.*

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.javasampleapproach.kotlin.firebase.realtimedb.model.Message

import java.text.SimpleDateFormat
import java.util.Calendar
import com.javasampleapproach.kotlin.firebase.realtimedb.model.User
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.ChildEventListener


class MessageActivity : AppCompatActivity() {

    private val TAG = "MessageActivity"
    private val REQUIRED = "Required"

    private var user: FirebaseUser? = null

    private var mDatabase: DatabaseReference? = null
    private var mMessageReference: DatabaseReference? = null
    private var mMessageListener: ChildEventListener? = null

    val messageList = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        mDatabase = FirebaseDatabase.getInstance().reference
        mMessageReference = FirebaseDatabase.getInstance().getReference("messages")
        user = FirebaseAuth.getInstance().currentUser

        firebaseListenerInit()

        btnSend.setOnClickListener {
            submitMessage()
            edtSentText.setText("")
        }

        btnBack.setOnClickListener {
            finish()
        }

        tvAuthor.text = ""
        tvTime.text = ""
        tvBody.text = ""
    }

    private fun firebaseListenerInit() {

        val childEventListener = object : ChildEventListener {

            override fun onChildAdded(dataSnapshot: DataSnapshot?, previousChildName: String?) {
                // A new message has been added
                // onChildAdded() will be called for each node at the first time
                val message = dataSnapshot!!.getValue(Message::class.java)
                messageList.add(message!!)

                Log.e(TAG, "onChildAdded:" + message.body)

                val latest = messageList[messageList.size - 1]

                tvAuthor.text = latest.author
                tvTime.text = latest.time
                tvBody.text = latest.body
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot?, previousChildName: String?) {
                Log.e(TAG, "onChildChanged:" + dataSnapshot!!.key)

                // A message has changed
                val message = dataSnapshot.getValue(Message::class.java)
                Toast.makeText(this@MessageActivity, "onChildChanged: " + message!!.body, Toast.LENGTH_SHORT).show()
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot?) {
                Log.e(TAG, "onChildRemoved:" + dataSnapshot!!.key)

                // A message has been removed
                val message = dataSnapshot.getValue(Message::class.java)
                Toast.makeText(this@MessageActivity, "onChildRemoved: " + message!!.body, Toast.LENGTH_SHORT).show()
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot?, previousChildName: String?) {
                Log.e(TAG, "onChildMoved:" + dataSnapshot!!.key)

                // A message has changed position
                val message = dataSnapshot.getValue(Message::class.java)
                Toast.makeText(this@MessageActivity, "onChildMoved: " + message!!.body, Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError?) {
                Log.e(TAG, "postMessages:onCancelled", databaseError!!.toException())
                Toast.makeText(this@MessageActivity, "Failed to load Message.", Toast.LENGTH_SHORT).show()
            }
        }

        mMessageReference!!.addChildEventListener(childEventListener)

        // copy for removing at onStop()
        mMessageListener = childEventListener
    }

    override fun onStop() {
        super.onStop()

        if (mMessageListener != null) {
            mMessageReference!!.removeEventListener(mMessageListener)
        }

        for (message in messageList) {
            Log.e(TAG, "listItem: " + message.body)
        }
    }

    private fun submitMessage() {
        val body = edtSentText.text.toString()

        if (TextUtils.isEmpty(body)) {
            edtSentText.error = REQUIRED
            return
        }

        // User data change listener
        mDatabase!!.child("users").child(user!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)

                if (user == null) {
                    Log.e(TAG, "onDataChange: User data is null!")
                    Toast.makeText(this@MessageActivity, "onDataChange: User data is null!", Toast.LENGTH_SHORT).show()
                    return
                }

                writeNewMessage(body)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.e(TAG, "onCancelled: Failed to read user!")
            }
        })
    }

    private fun writeNewMessage(body: String) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().time)
        val message = Message(getUsernameFromEmail(user!!.email), body, time)

        val messageValues = message.toMap()
        val childUpdates = HashMap<String, Any>()

        val key = mDatabase!!.child("messages").push().key

        childUpdates.put("/messages/" + key, messageValues)
        childUpdates.put("/user-messages/" + user!!.uid + "/" + key, messageValues)

        mDatabase!!.updateChildren(childUpdates)
    }

    private fun getUsernameFromEmail(email: String?): String {
        return if (email!!.contains("@")) {
            email.split("@".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
        } else {
            email
        }
    }
}
