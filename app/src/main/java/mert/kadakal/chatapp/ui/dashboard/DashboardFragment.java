package mert.kadakal.chatapp.ui.dashboard;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

import mert.kadakal.chatapp.databinding.FragmentDashboardBinding;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private Button oda_olstr;
    private Button odaya_gir;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        DashboardViewModel dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        oda_olstr = binding.odaOlustur;
        odaya_gir = binding.odayaGir;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        odaya_gir.setText("Odadan Çık");
        if (sharedPreferences.getString("oda", "").equals("")) {
            odaya_gir.setText("Odaya Gir");
        }

        oda_olstr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Bilgi Girin");

                // EditText ekleyerek kullanıcının veri girmesini sağla
                final EditText input = new EditText(getContext());
                builder.setView(input);
                Map<String, Object> oda = new HashMap<>();

                builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String userInput = input.getText().toString();

                        oda.put("isim", userInput); // Zaman damgası ekleniyor

                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setTitle("Bilgi Girin");

                        // EditText ekleyerek kullanıcının veri girmesini sağla
                        final EditText input = new EditText(getContext());
                        builder.setView(input);

                        builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String userInput = input.getText().toString();
                                oda.put("şifre", userInput); // Zaman damgası ekleniyor

                                // Firebase Firestore'a ekleme işlemi
                                db.collection("odalar")
                                        .add(oda) // Belge ekleniyor
                                        .addOnSuccessListener(documentReference -> {
                                            String odaId = documentReference.getId(); // Eklenen belgenin ID'si alınıyor

                                            // SharedPreferences'a kaydet
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString("oda", odaId); // Oda ID'sini kaydediyoruz
                                            editor.putString("konum", sharedPreferences.getString("hesap", "")); // Ekstra veri ekleyebilirsin
                                            editor.apply(); // Değişiklikleri kaydet
                                        });

                            }
                        });

                        builder.setNegativeButton("İptal", null);
                        builder.show();
                    }
                });

                builder.setNegativeButton("İptal", null);
                builder.show();
            }
        });

        odaya_gir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (odaya_gir.getText().equals("Odaya Gir")) {
                    if (sharedPreferences.getString("hesap","").equals("")) {
                        Toast.makeText(getContext(), "Bir hesaba giriş yapmalısınız", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setTitle("Bilgi Girin");

                    // Kullanıcıdan isim almak için EditText
                    final EditText input = new EditText(getContext());
                    builder.setView(input);

                    builder.setPositiveButton("Tamam", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String userInput = input.getText().toString().trim();

                            db.collection("odalar")
                                    .whereEqualTo("isim", userInput) // "isim" eşleşenleri getir
                                    .get()
                                    .addOnSuccessListener(queryDocumentSnapshots -> {
                                        if (!queryDocumentSnapshots.isEmpty()) {
                                            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                                String correctPassword = document.getString("şifre"); // Doğru şifreyi al
                                                String odaID = document.getId(); // Firestore document ID

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
                                                            editor.putString("oda", odaID); // Firestore'daki belge ID'sini kaydet
                                                            editor.apply();

                                                            odaya_gir.setText("Odadan Çık");
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
                    // SharedPreferences'a kaydet
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("oda", ""); // Firestore'daki belge ID'sini kaydet
                    editor.apply();

                    odaya_gir.setText("Odaya Gir");
                }
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