package com.example.datve.movie;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.datve.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ComingSoonFragment extends Fragment {

    private RecyclerView recyclerView;
    private MovieAdapter movieAdapter;
    private List<Movie> comingSoonList;

    private static final String MOVIES_API_URL = "http://10.0.2.2:8080/movies";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_coming_soon, container, false);

        recyclerView = view.findViewById(R.id.recycler_view_coming_soon);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        comingSoonList = new ArrayList<>();
        movieAdapter = new MovieAdapter(getContext(), comingSoonList);
        recyclerView.setAdapter(movieAdapter);

        fetchMovies();

        return view;
    }

    private void fetchMovies() {
        RequestQueue queue = Volley.newRequestQueue(requireContext());

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, MOVIES_API_URL, null,
                response -> {
                    try {
                        comingSoonList.clear();
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject movieObject = response.getJSONObject(i);
                            // Lớp Movie sẽ tự xử lý việc lấy 'hot' và 'minAge'
                            Movie movie = new Movie(movieObject);

                            // Lọc phim sắp chiếu (active == 0)
                            if (movie.getActive() == 0) {
                                comingSoonList.add(movie);
                            }
                        }
                        movieAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(getContext(), "Lỗi xử lý dữ liệu phim!", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(getContext(), "Không thể tải danh sách phim. Vui lòng kiểm tra kết nối.", Toast.LENGTH_SHORT).show();
                }
        );

        queue.add(jsonArrayRequest);
    }
}
