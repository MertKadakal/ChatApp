package mert.kadakal.chatapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class MessageAdapter extends ArrayAdapter<String> {

    private Context context;
    private List<String> messages;
    private SharedPreferences sharedPreferences;

    public MessageAdapter(Context context, List<String> messages, SharedPreferences sharedPreferences) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.messages_item, parent, false);
        }

        String content = getItem(position);
        String[] parts = content.split("<prs>");

        if (parts.length > 1) {
            String person = parts[1];
            String message = parts[2];

            TextView messager_isim = convertView.findViewById(R.id.messeger_isim);
            TextView messager_message = convertView.findViewById(R.id.messeger_message);
            //ViewGroup parentView = (ViewGroup) messager_isim.getParent();

            // Kullanıcının kendi mesajları sağda olmalı
            if (person.equals(sharedPreferences.getString("hesap", ""))) {
                messager_isim.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                messager_message.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
            } else {
                messager_isim.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                messager_message.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
            }

            if (position == 0 || !getItem(position - 1).split("<prs>")[1].equals(person)) {
                messager_isim.setText(person);
            } else {
                ((ViewGroup) messager_isim.getParent()).removeView(messager_isim);
            }

            messager_message.setText(message);
        }

        return convertView;
    }
}
