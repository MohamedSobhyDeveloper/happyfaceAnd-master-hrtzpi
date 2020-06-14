package com.hrtzpi.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.hrtzpi.R;
import com.hrtzpi.adapters.CategoryAdapter;
import com.hrtzpi.adapters.ImageSliderAdapter;
import com.hrtzpi.helpers.CallbackRetrofit;
import com.hrtzpi.helpers.Loading;
import com.hrtzpi.helpers.RetrofitModel;
import com.hrtzpi.models.category_models.CategoryResponse;
import com.hrtzpi.models.category_models.DataItem;
import com.hrtzpi.models.slider_models.Data;
import com.hrtzpi.models.slider_models.SliderItem;
import com.hrtzpi.models.slider_models.SliderResponse;
import com.wang.avi.AVLoadingIndicatorView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

import static com.hrtzpi.helpers.StaticMembers.changeDots;

public class MainFragment extends Fragment {

    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.swipe)
    SwipeRefreshLayout swipe;
    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.noItem)
    TextView noItem;
    @BindView(R.id.indicator)
    LinearLayout indicator;
    CategoryAdapter adapter;
    private ImageSliderAdapter sliderAdapter;
    private List<DataItem> list;
    private List<SliderItem> sliderList;
    private CategoryAdapter.OnCategorySelectedListener listener;
    private Handler handler;
    private Runnable runnable;
    Loading loading;

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null)
            handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        loading=new Loading(getActivity());
        list = new ArrayList<>();
        sliderList = new ArrayList<>();

        adapter = new CategoryAdapter(getContext(), list, listener);
        sliderAdapter = new ImageSliderAdapter(getContext(), sliderList, listener);
        recycler.setAdapter(adapter);
        pager.setOffscreenPageLimit(1);
        pager.setAdapter(sliderAdapter);
        changeDots(0, sliderAdapter.getRealCount(), indicator, getContext());
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                changeDots(position % sliderAdapter.getRealCount(), sliderAdapter.getRealCount(), indicator, getContext());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        Timer timer = new Timer(); // At this line a new Thread will be created
        handler = new Handler();
        runnable = () -> {
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (getActivity() != null)
                        Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                            int page = pager.getCurrentItem();
                            page++;
                            if (page == Integer.MAX_VALUE)
                                page = 0;
                            pager.setCurrentItem(page, true);
                        });
                }
            }, 0, 2000); // delay
        };
        handler.postDelayed(runnable, 2000);
        swipe.setOnRefreshListener(this::getCats);
        getCats();
        getSlider();
    }

    void getSlider() {

        if (loading!=null){
            loading.show();

        }
        Call<SliderResponse> call = RetrofitModel.getApi(getContext()).getSlider();
        call.enqueue(new CallbackRetrofit<SliderResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<SliderResponse> call, @NotNull Response<SliderResponse> response) {
                if (getContext() != null) {
                    if (loading!=null&&loading.isShowing()){
                        loading.dismiss();

                    }

                    if (response.isSuccessful()) {
                        if (response.body() != null && response.body().isStatus()) {
                            Data data = response.body().getData();
                            if (data.getSlider1() != null) {
                                sliderList.addAll(data.getSlider1());
                            }
                            sliderAdapter.notifyDataSetChanged();
                            changeDots(0, sliderAdapter.getRealCount(), indicator, getContext());
                        }
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<SliderResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

            }
        });

    }

    void getCats() {

        if (loading!=null){
            loading.show();

        }
        noItem.setVisibility(View.GONE);
        Call<CategoryResponse> call = RetrofitModel.getApi(getActivity()).getCategories();
        call.enqueue(new CallbackRetrofit<CategoryResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<CategoryResponse> call, @NotNull Response<CategoryResponse> response) {
                if (getContext() != null) {
                    if (loading!=null&&loading.isShowing()){
                        loading.dismiss();

                    }
                    swipe.setRefreshing(false);
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        list.clear();
                        list.addAll(response.body().getData());
                        adapter.notifyDataSetChanged();
                        if (list.isEmpty())
                            noItem.setVisibility(View.VISIBLE);
                        else noItem.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call<CategoryResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();
                }
                swipe.setRefreshing(false);
                noItem.setVisibility(View.GONE);
            }
        });
    }

    @NotNull
    public static MainFragment getInstance(CategoryAdapter.OnCategorySelectedListener listener) {
        MainFragment mainFragment = new MainFragment();
        mainFragment.listener = listener;
        return mainFragment;
    }
}
