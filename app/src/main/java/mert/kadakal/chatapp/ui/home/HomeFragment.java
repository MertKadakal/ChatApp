package mert.kadakal.chatapp.ui.home;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Html;
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
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mert.kadakal.chatapp.MainActivity;
import mert.kadakal.chatapp.R;
import mert.kadakal.chatapp.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private ArrayList<String> dataList;
    private ArrayAdapter<String> adapter;
    private Button mesaj;
    private String prs = "a";
    private TextView oda;

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

                    adapter.notifyDataSetChanged(); // Listeyi güncelle
                });

        // Özelleştirilmiş ArrayAdapter sayesinde mesajların görüntülenmesi
        adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_list_item_1, dataList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                // Veriyi al
                String content = getItem(position);

                // Split işlemi ile content'i ikiye ayırıyoruz
                String[] parts = content.split("<prs>");

                // Eğer split başarılı olduysa ve iki parça varsa
                if (parts.length > 1) {
                    String person = parts[1]; // İlk kısmı person olarak al
                    String message = String.valueOf(Html.fromHtml("---" + person+"<br>"+parts[2]));

                    // TextView'leri tanımla
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);

                    // Eğer "a" ise sağa yasla
                    if (person.equals(sharedPreferences.getString("hesap", ""))) {
                        // Sağa yasla
                        textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                    } else {
                        // Sola yasla
                        textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
                    }

                    // Mesajı yerleştir
                    textView.setText(message);
                }

                return view;
            }
        };

        listView.setAdapter(adapter);

        mesaj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Bilgi Girin");

                // EditText ekleyerek kullanıcının veri girmesini sağla
                final EditText input = new EditText(getContext());
                builder.setView(input);

                builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userInput = input.getText().toString();

                        // Yeni mesaj verisi
                        String oda = sharedPreferences.getString("oda", ""); // Varsayılan değer: "Varsayılan Ad"
                        String konum = sharedPreferences.getString("hesap", ""); // Varsayılan değer: 0

                        Map<String, Object> message = new HashMap<>();
                        message.put("content", oda + "<prs>" + konum + "<prs>" + userInput);
                        message.put("timestamp", System.currentTimeMillis()); // Zaman damgası ekleniyor

                        // Firestore'a ekle
                        db.collection("messages").add(message);
                    }
                });


                builder.setNegativeButton("İptal", null);
                builder.show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
