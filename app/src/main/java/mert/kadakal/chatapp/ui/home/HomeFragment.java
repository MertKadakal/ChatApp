package mert.kadakal.chatapp.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mert.kadakal.chatapp.MessageAdapter;
import mert.kadakal.chatapp.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ArrayList<String> dataList;
    private MessageAdapter adapter;
    private Button mesaj;
    private TextView oda;
    private TextView oda_yok;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // bileşenleri tanımla
        ListView listView = binding.listHome;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        dataList = new ArrayList<>();
        mesaj = binding.mesajYaz;
        oda = binding.oda;
        adapter = new MessageAdapter(requireContext(), dataList, sharedPreferences);
        oda_yok = binding.odaYok;

        if (sharedPreferences.getString("oda", "").equals("")) {
            mesaj.setVisibility(View.INVISIBLE);
            oda_yok.setVisibility(View.VISIBLE);
        }


        String odaId = sharedPreferences.getString("oda", "");
        if (!odaId.isEmpty()) {
            db.collection("odalar")
                    .document(odaId) // Belge ID'sine göre erişim sağla
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String isim = documentSnapshot.getString("isim"); // "isim" alanını al

                            oda.setText(isim);
                        }
                    });
        }

        // sürekli olarak veritabanından mesajlar alınarak sayfa güncel tutulur
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING) // Zaman damgasına göre sırala
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Log.w("TAG", "Error listening to updates.", e);
                        return;
                    }

                    dataList.clear(); // Listeyi temizle

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        String content = document.getString("content");
                        if (content != null && content.split("<prs>")[0].equals(sharedPreferences.getString("oda", ""))) {
                            dataList.add(content); // Mesajı listeye ekle
                        }
                    }

                    Log.d("amount", String.valueOf(dataList.size()));

                    adapter.notifyDataSetChanged(); // Listeyi güncelle
                });


        listView.setAdapter(adapter);

        mesaj.setOnClickListener(v -> {
            if (sharedPreferences.getString("oda", "").equals("")) {
                Toast.makeText(getContext(), "Bir odaya giriş yapmalısınız", Toast.LENGTH_SHORT).show();
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Mesaj Girin");

            // EditText ekleyerek kullanıcının veri girmesini sağla
            final EditText input = new EditText(getContext());
            builder.setView(input);

            builder.setPositiveButton("Gönder", (dialog, which) -> {
                String userInput = input.getText().toString();

                // Yeni mesaj verisi
                String oda = sharedPreferences.getString("oda", ""); // Varsayılan değer: "Varsayılan Ad"
                String konum = sharedPreferences.getString("hesap", ""); // Varsayılan değer: 0

                Map<String, Object> message = new HashMap<>();
                message.put("content", oda + "<prs>" + konum + "<prs>" + userInput);
                message.put("timestamp", System.currentTimeMillis()); // Zaman damgası ekleniyor

                // Firestore'a ekle
                db.collection("messages").add(message);
            });


            builder.setNegativeButton("İptal", null);
            builder.show();
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
