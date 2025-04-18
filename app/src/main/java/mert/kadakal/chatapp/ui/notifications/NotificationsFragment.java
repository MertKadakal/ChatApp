package mert.kadakal.chatapp.ui.notifications;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Source;

import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.Map;

import mert.kadakal.chatapp.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;
    private Button hesap_olstr;
    private Button giris_yap;
    private TextView hesap_ismi;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        hesap_olstr = binding.hesapOlstr;
        giris_yap = binding.girisYap;
        hesap_ismi = binding.hesapIsmi;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        CollectionReference koleksiyon = db.collection("hesaplar");

        koleksiyon.get(Source.SERVER).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String isim = document.getString("isim");
                    Log.d("Firestore", "İsim: " + isim);
                }
            } else {
                Log.e("Firestore", "Hata: ", task.getException());
            }
        });

        giris_yap.setText("Çıkış Yap");
        if (sharedPreferences.getString("hesap","").equals("")) {
            giris_yap.setText("Giriş Yap");
        }

        hesap_ismi.setText(sharedPreferences.getString("hesap", ""));

        hesap_olstr.setOnClickListener(v -> {
            if (!sharedPreferences.getString("hesap", "").equals("")) {
                Toast.makeText(getContext(), "Hesaptan çıkış yapmalısınız", Toast.LENGTH_SHORT).show();
                return;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Hesap İsmi Girin");

            // EditText ekleyerek kullanıcının veri girmesini sağla
            final EditText input = new EditText(getContext());
            builder.setView(input);
            Map<String, Object> hesap = new HashMap<>();

            builder.setPositiveButton("Tamam", (dialog, which) -> {
                String userInput = input.getText().toString().trim();

                if (userInput.isEmpty()) {
                    Toast.makeText(getContext(), "Hesap ismi boş olamaz!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Firestore'da aynı isme sahip bir hesap olup olmadığını kontrol et
                db.collection("hesaplar")
                        .whereEqualTo("isim", userInput)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                Toast.makeText(getContext(), "Bu isimde bir hesap zaten var!", Toast.LENGTH_SHORT).show();
                            } else {
                                hesap.put("isim", userInput);

                                AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                                builder1.setTitle("Şifre Girin");

                                // EditText ekleyerek kullanıcının veri girmesini sağla
                                final EditText input1 = new EditText(getContext());
                                builder1.setView(input1);

                                builder1.setPositiveButton("Tamam", (dialog1, which1) -> {
                                    String userInput1 = input1.getText().toString();
                                    hesap.put("şifre", userInput1);

                                    // Firebase Firestore'a ekleme işlemi
                                    db.collection("hesaplar")
                                            .add(hesap)
                                            .addOnSuccessListener(documentReference -> {
                                                // SharedPreferences'a kaydet
                                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                                editor.putString("hesap", userInput);
                                                editor.putString("oda", "");
                                                editor.apply();

                                                hesap_ismi.setText(sharedPreferences.getString("hesap", ""));
                                                giris_yap.setText("Çıkış Yap");
                                            });

                                });

                                builder1.setNegativeButton("İptal", null);
                                builder1.show();
                            }
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(getContext(), "Veritabanı hatası: " + e.getMessage(), Toast.LENGTH_SHORT).show());

            });

            builder.setNegativeButton("İptal", null);
            builder.show();
        });


        giris_yap.setOnClickListener(v -> {
            if (giris_yap.getText().toString().equals("Giriş Yap")) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Hesap İsmini Girin");

                // Kullanıcıdan isim almak için EditText
                final EditText input = new EditText(getContext());
                builder.setView(input);

                builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userInput = input.getText().toString().trim();

                        db.collection("hesaplar")
                                .whereEqualTo("isim", userInput) // "isim" eşleşenleri getir
                                .get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    if (!queryDocumentSnapshots.isEmpty()) {
                                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                            String correctPassword = document.getString("şifre"); // Doğru şifreyi al

                                            // Yeni bir şifre giriş dialogu aç
                                            AlertDialog.Builder passwordDialog = new AlertDialog.Builder(getContext());
                                            passwordDialog.setTitle("Şifre Girin");

                                            final EditText passwordInput = new EditText(getContext());
                                            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                            passwordDialog.setView(passwordInput);

                                            passwordDialog.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    String enteredPassword = passwordInput.getText().toString();

                                                    if (enteredPassword.equals(correctPassword)) {
                                                        Toast.makeText(getContext(), "Giriş Yapıldı", Toast.LENGTH_SHORT).show();

                                                        // SharedPreferences'a kaydet
                                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                                        editor.putString("hesap", userInput); // Kullanıcı adını kaydet
                                                        editor.putString("oda", ""); // Firestore'daki belge ID'sini kaydet
                                                        editor.apply();
                                                        hesap_ismi.setText(sharedPreferences.getString("hesap", ""));

                                                        giris_yap.setText("Çıkış Yap");
                                                    } else {
                                                        Toast.makeText(getContext(), "Hatalı şifre!", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });

                                            passwordDialog.setNegativeButton("İptal", null);
                                            passwordDialog.show();
                                        }
                                    } else {
                                        Toast.makeText(getContext(), "Kullanıcı ismi bulunamadı!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Hata oluştu!", Toast.LENGTH_SHORT).show());
                    }
                });

                builder.setNegativeButton("İptal", null);
                builder.show();
            } else {
                // Çıkış yapıldığında SharedPreferences'ı temizle
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("hesap", ""); // Kullanıcı adı temizleniyor
                editor.putString("oda", ""); // Oda ID temizleniyor
                editor.apply();
                hesap_ismi.setText("");

                Toast.makeText(getContext(), "Çıkış yapıldı!", Toast.LENGTH_SHORT).show();
                giris_yap.setText("Giriş Yap");
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