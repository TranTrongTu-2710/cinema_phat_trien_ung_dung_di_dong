package com.example.datve.notification;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;
import com.example.datve.user.SessionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> notificationList;
    private SessionManager sessionManager;
    private RequestQueue requestQueue;

    private static final String NEWS_BASE_URL = "http://10.0.2.2:8080/news";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_notification);
        sessionManager = new SessionManager(getContext());
        requestQueue = Volley.newRequestQueue(getContext());
        notificationList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationAdapter = new NotificationAdapter(getContext(), notificationList);
        recyclerView.setAdapter(notificationAdapter);

        fetchNotifications();

        return view;
    }

    private void fetchNotifications() {
        String url;
        final boolean isLoggedIn = sessionManager.isLoggedIn();
        String userId = sessionManager.getUserId();

        if (isLoggedIn && userId != null) {
            url = NEWS_BASE_URL + "/visible/" + userId;
        } else {
            url = NEWS_BASE_URL + "/public";
        }

        Log.d("NotificationFragment", "Fetching URL: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.has("items")) {
                            Log.e("NotificationFragment", "Response JSON không chứa key 'items'");
                            return;
                        }

                        JSONArray itemsArray = response.getJSONArray("items");

                        notificationList.clear();
                        for (int i = 0; i < itemsArray.length(); i++) {
                            JSONObject notificationObject = itemsArray.getJSONObject(i);
                            Notification notification = new Notification(notificationObject);

                            if (notification.getActive() == 1) {
                                notificationList.add(notification);
                            }
                        }

                        // Cập nhật lại giao diện của RecyclerView
                        notificationAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu thông báo", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Không thể tải thông báo. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                if (isLoggedIn) {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Bearer " + sessionManager.getToken());
                    return headers;
                }
                return super.getHeaders();
            }
        };

        requestQueue.add(jsonObjectRequest);
    }
}
