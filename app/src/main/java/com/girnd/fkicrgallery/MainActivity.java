package com.girnd.fkicrgallery;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private RecyclerView mRecyclerViewPhotos;
    private List<GalleryItem> mItemList;
    private int mPageNumber = 1;

    private  PhotosAdapter mPhotosAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerViewPhotos = findViewById(R.id.recyclerViewPhoto);


        mItemList = new ArrayList<>();


    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        FlickrGetter flickrGetter = new FlickrGetter();
        LoadPhotoTask loadPhotoTask = new LoadPhotoTask();
        loadPhotoTask.execute(flickrGetter.getSearchUri("cat", mPageNumber));
        GridLayoutManager gridLayoutManager = new GridLayoutManager(MainActivity.this,3);
        mPhotosAdapter = new PhotosAdapter();
        mRecyclerViewPhotos.setAdapter(mPhotosAdapter);
        mRecyclerViewPhotos.setLayoutManager(gridLayoutManager);
    }

    private class PhotoHolder extends RecyclerView.ViewHolder{
        private ImageView holder;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            holder = (ImageView) itemView;
        }

        public void bindPhotoHolder(GalleryItem item){
            Picasso.get()
                    .load(item.getImageUrl())
                    .resize(mRecyclerViewPhotos.getWidth()/3, mRecyclerViewPhotos.getWidth()/3)
                    .centerCrop()
                    .into(holder);
        }
    }

    private class PhotosAdapter extends RecyclerView.Adapter<PhotoHolder>{

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.image_view_holder, viewGroup, false);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(mRecyclerViewPhotos.getRight() / 3, mRecyclerViewPhotos.getRight() / 3);
            view.setLayoutParams(params);
            return new PhotoHolder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder photoHolder, int i) {
            photoHolder.bindPhotoHolder(mItemList.get(i));
        }

        @Override
        public int getItemCount() {
            return mItemList.size();
        }

        @Override
        public void onViewAttachedToWindow(@NonNull PhotoHolder holder) {
            super.onViewAttachedToWindow(holder);
            FlickrGetter flickrGetter = new FlickrGetter();
            if (holder.getLayoutPosition() == mItemList.size() - 1 && mPageNumber <= 10){
                LoadPhotoTask loadPhotoTask = new LoadPhotoTask();
                loadPhotoTask.execute(flickrGetter.getSearchUri("cat", ++mPageNumber));
            }

        }
    }

    private class FlickrGetter {
        private final String TAG = "FlickrTag";
        private final String METHOD_GET_ALL = "flickr.photos.getRecent";
        private final String METHOD_SEARCH = "flickr.photos.search";
        private final String API_KEY = "0459f37aaa49617378a4b38953fb092c";
        private HttpURLConnection connection;

        private String buildURI(String method, String query, int pageNumber){
            if (method.equals(METHOD_GET_ALL)){
                String uri = Uri.parse("https://api.flickr.com/services/rest/")
                        .buildUpon()
                        .appendQueryParameter("method", method)
                        .appendQueryParameter("api_key", API_KEY)
                        .appendQueryParameter("format", "json")
                        .appendQueryParameter("nojsoncallback", "1")
                        .appendQueryParameter("page", String.valueOf(pageNumber))
                        .appendQueryParameter("extras", "url_s")
                        .build().toString();
                return uri;
            }
            if (method.equals(METHOD_SEARCH)){
                String uri = Uri.parse("https://api.flickr.com/services/rest/")
                        .buildUpon()
                        .appendQueryParameter("method", method)
                        .appendQueryParameter("api_key", API_KEY)
                        .appendQueryParameter("format", "json")
                        .appendQueryParameter("nojsoncallback", "1")
                        .appendQueryParameter("page", String.valueOf(pageNumber))
                        .appendQueryParameter("extras", "url_s")
                        .appendQueryParameter("text", query)
                        .build().toString();
                return uri;
            }
            return null;
        }

        private String getSearchUri(String query, int pageNumber){
            return buildURI(METHOD_SEARCH, query, pageNumber);
        }

        private String getFetchUri(int pageNumber){
            return buildURI(METHOD_GET_ALL, null, pageNumber);
        }

        private byte[] getBytesFlickr (String specUri) throws IOException {
            try {
                URL url = new URL(specUri);
                connection = (HttpURLConnection) url.openConnection();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = connection.getInputStream();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    Log.i(TAG, "Connection error");
                }else {
                    Log.i(TAG, "Connection response is ok");
                }
                int bytesRead = 0;
                byte[] buffer = new byte[1024];
                while ((bytesRead = in.read(buffer)) > 0){
                    out.write(buffer, 0, bytesRead);
                }
                out.close();
                return out.toByteArray();
            } finally {
                connection.disconnect();
            }
        }

        private void getItemsFromFlickr(String uri) throws IOException{
//            String uri = Uri.parse("https://api.flickr.com/services/rest/")
//                    .buildUpon()
//                    .appendQueryParameter("method", METHOD)
//                    .appendQueryParameter("api_key", API_KEY)
//                    .appendQueryParameter("format", "json")
//                    .appendQueryParameter("nojsoncallback", "1")
//                    .appendQueryParameter("page", String.valueOf(pageNumber))
//                    .appendQueryParameter("extras", "url_s")
//                    .build().toString();
            byte[] bytes = getBytesFlickr(uri);
            String JSONString = new String(bytes);
            Log.i(TAG, JSONString);
            try {
                JSONObject jsonObjectGlobal = new JSONObject(JSONString);
                JSONObject jsonObjectPhotos = jsonObjectGlobal.getJSONObject("photos");
                JSONArray jsonArrayPhoto = jsonObjectPhotos.getJSONArray("photo");
                for (int i = 0; i < jsonArrayPhoto.length(); i++){
                    JSONObject jsonObjectPhoto = jsonArrayPhoto.getJSONObject(i);
                    if (jsonObjectPhoto.getString("url_s") != null){
                        GalleryItem item = new GalleryItem();
                        item.setId(jsonObjectPhoto.getString("id"));
                        item.setTitle(jsonObjectPhoto.getString("title"));
                        item.setImageUrl(jsonObjectPhoto.getString("url_s"));
                        mItemList.add(item);

                    }
                }
            } catch (JSONException e) {
                Log.i(TAG, "Json exception");
                e.printStackTrace();
            }

        }


    }
    private class LoadPhotoTask extends AsyncTask<String, Void, Void>{

        @Override
        protected Void doInBackground(String... strings) {
            FlickrGetter flickrGetter = new FlickrGetter();
            try {
                flickrGetter.getItemsFromFlickr(strings[0]);
            } catch (IOException e) {
                Log.i("LoadError","Load error");
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mPhotosAdapter.notifyDataSetChanged();
        }
    }
}
