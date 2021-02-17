package com.example.myapplicationas;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.myapplicationas.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference mDatabaseReference;
    private StorageReference mStorageReference;
    private ActivityMainBinding bnd;

    private static final int RC_GALLERY = 21;
    private static final int RC_CAMERA = 22;

    private static final int RP_CAMERA = 121;
    private static final int RP_STORAGE = 122;

    private static final String IMAGE_DIRECTORY = "/MyPhotoApp";
    private static final String MY_PHOTO = "my_photo";

    private static final String PATH_PROFILE = "profile";
    private static final String PATH_PHOTO_URL = "photoUrl";

    private String mCurrentPhotoPath;
    private Uri mPhotoSelectedUri;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bnd = bnd.inflate(getLayoutInflater());
        setContentView(bnd.getRoot());
        //setContentView(R.layout.activity_main);
        //BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        //AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
        //        R.id.navigation_gallery, R.id.navigation_camera)
        //        .build();
        //NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        //NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        //NavigationUI.setupWithNavController(navView, navController);
        BottomNavigationView navigation = (BottomNavigationView) bnd.navView;
        navigation.setOnNavigationItemSelectedListener(monNavigationItemSelectedListener);
        bnd.btnUpload.setOnClickListener(UploadPhoto);
        bnd.btnDelete.setOnClickListener(DeletePhoto);
        configFirebase();
        configPhotoProfile();

        final RequestOptions option = new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL);

        Glide.with(this).load("https://raw.githubusercontent.com/bumptech/glide/master/static/glide_logo.png")
                .apply(option)
                .into(bnd.imgPhoto);
    }

    private View.OnClickListener DeletePhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mStorageReference.child(PATH_PROFILE).child(MY_PHOTO).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mDatabaseReference.removeValue();
                    bnd.imgPhoto.setImageBitmap(null);
                    bnd.btnDelete.setVisibility(View.GONE);
                    Snackbar.make(bnd.container,R.string.main_message_delete_success,Snackbar.LENGTH_LONG).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Snackbar.make(bnd.container,R.string.main_message_delete_error,Snackbar.LENGTH_LONG).show();
                }
            });
        }
    };

    private void configFirebase() {
        mStorageReference = FirebaseStorage.getInstance().getReference();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabaseReference = database.getReference().child(PATH_PROFILE).child(PATH_PHOTO_URL);

    }

    private void configPhotoProfile() {
        /*mStorageReference.child(PATH_PROFILE).child(MY_PHOTO).getDownloadUrl()
                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        final RequestOptions option = new RequestOptions().centerCrop().diskCacheStrategy(DiskCacheStrategy.ALL);

                        Glide.with(MainActivity.this).load("uri")
                                .apply(option)
                                .into(bnd.imgPhoto);

                        bnd.btnDelete.setVisibility(View.VISIBLE);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                bnd.btnDelete.setVisibility(View.GONE);
                Snackbar.make(bnd.container,R.string.main_message_error_notFound,Snackbar.LENGTH_LONG).show();
            }
        });*/

        mDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final RequestOptions options = new RequestOptions()
                        .centerCrop()
                        .diskCacheStrategy(DiskCacheStrategy.ALL);

                Glide.with(MainActivity.this)
                        .load(snapshot.getValue())
                        .apply(options)
                        .into(bnd.imgPhoto);

                bnd.btnDelete.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                bnd.btnDelete.setVisibility(View.GONE);
                Snackbar.make(bnd.container,R.string.main_message_error_notFound,Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private View.OnClickListener UploadPhoto = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            StorageReference profileReference = mStorageReference.child(PATH_PROFILE);
            StorageReference photoReference = profileReference.child(MY_PHOTO);
            photoReference.putFile(mPhotoSelectedUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Snackbar.make(bnd.container,R.string.main_message_upload_success,Snackbar.LENGTH_LONG).show();
                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            savePhotoUrl(uri);
                            bnd.btnDelete.setVisibility(View.VISIBLE);
                            bnd.message.setText(R.string.main_message_done);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Snackbar.make(bnd.container,R.string.main_message_upload_error,Snackbar.LENGTH_LONG).show();
                        }
                    });
                }
            });
        }
    };

    private void savePhotoUrl(Uri uri) {
        mDatabaseReference.setValue(uri.toString());
    }


    private BottomNavigationView.OnNavigationItemSelectedListener monNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.navigation_gallery:
                    bnd.message.setText(R.string.main_label_gallery);
                    CheckPermissiontoApp(Manifest.permission.READ_EXTERNAL_STORAGE,RP_STORAGE);
                    //fromGallery();
                    return  true;
                case R.id.navigation_camera:
                    bnd.message.setText(R.string.main_label_camera);
                    CheckPermissiontoApp(Manifest.permission.CAMERA,RP_CAMERA);
                    //fromCamera();
                    return  true;
            }
            return false;
        }
    };

    private void CheckPermissiontoApp(String permission, int requestPermission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this,new String[]{permission},requestPermission);
                return;
            }
        }

        switch (requestPermission)
        {
            case RP_STORAGE:
                fromGallery();
                break;
            case RP_CAMERA:
                dispatchtakePictureIntent();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
        {
            switch (requestCode)
            {
                case RP_STORAGE:
                    fromGallery();
                    break;

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode)
            {
                case RC_GALLERY:
                    if(data != null)
                    {
                        try {
                            mPhotoSelectedUri = data.getData();
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),mPhotoSelectedUri);
                            bnd.imgPhoto.setImageBitmap(bitmap);
                            bnd.btnDelete.setVisibility(View.GONE);
                            bnd.message.setText(R.string.main_message_question_upload);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                case RC_CAMERA:
                    /*Bundle extras = data.getExtras();
                    Bitmap bitmap = (Bitmap)extras.get("data");*/
                    mPhotoSelectedUri = addPicGallery();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),mPhotoSelectedUri);
                        bnd.imgPhoto.setImageBitmap(bitmap);
                        bnd.btnDelete.setVisibility(View.GONE);
                        bnd.message.setText(R.string.main_message_question_upload);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    private Uri addPicGallery() {
        Intent mediascan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(mCurrentPhotoPath);
        Uri ContentUri = Uri.fromFile(file);
        mediascan.setData(ContentUri);
        this.sendBroadcast(mediascan);
        mCurrentPhotoPath = null;
        return ContentUri;
    }

    private void fromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent,RC_GALLERY);
    }

    private void fromCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent,RC_CAMERA);
    }

    private void dispatchtakePictureIntent() {
        Intent takepicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        PackageManager data =  getPackageManager();
        if(takepicture.resolveActivity( getPackageManager()) != null)
        {
            File photoFile;
            photoFile = createImageFile();
            if(photoFile != null)
            {
                Uri photoUri = FileProvider.getUriForFile(this,"com.example.myapplicationas",photoFile);
                takepicture.putExtra(MediaStore.EXTRA_OUTPUT,photoUri);
                startActivityForResult(takepicture,RC_CAMERA);
            }
        }
    }

    private File createImageFile() {
        final String timeStamp = new SimpleDateFormat("dd-MM-yyyy_HHmmmss", Locale.ROOT).format(new Date());
        final String imgFileName = MY_PHOTO + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(imgFileName,".jpg",storageDir);
            mCurrentPhotoPath = image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  image;
    }

}