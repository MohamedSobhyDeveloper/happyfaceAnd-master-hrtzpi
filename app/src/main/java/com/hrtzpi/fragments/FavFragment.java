package com.hrtzpi.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.hrtzpi.R;
import com.hrtzpi.activities.LogInActivity;
import com.hrtzpi.adapters.FavoritesAdapter;
import com.hrtzpi.helpers.CallbackRetrofit;
import com.hrtzpi.helpers.Loading;
import com.hrtzpi.helpers.PrefManager;
import com.hrtzpi.helpers.RetrofitModel;
import com.hrtzpi.helpers.StaticMembers;
import com.hrtzpi.models.search_products.Product;
import com.hrtzpi.models.search_products.ProductsResponse;
import com.wang.avi.AVLoadingIndicatorView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

public class FavFragment extends Fragment {

    @BindView(R.id.recycler)
    RecyclerView recycler;
    @BindView(R.id.swipe)
    SwipeRefreshLayout swipe;
    @BindView(R.id.noItem)
    TextView noItem;
    FavoritesAdapter adapter;
    List<Product> productList;
    int page = 1, maxPage;
    boolean reachBottom;
    private boolean isRefresh;
    HashMap<String, String> params;
    Loading loading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        loading=new Loading(getActivity());
        productList = new ArrayList<>();
        adapter = new FavoritesAdapter(getContext(), productList, loading, recycler,getActivity());
        recycler.setAdapter(adapter);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!recyclerView.canScrollVertically(1) && !reachBottom && page < maxPage) {
                    reachBottom = true;
                    page++;
                    getProducts();
                }
            }
        });
        getProducts();
        swipe.setOnRefreshListener(this::getProducts);
    }

    public void getProducts() {
        getProducts(true);
    }

    public void getProducts(boolean isRef) {
        if (loading!=null){
            loading.show();

        }
        if (PrefManager.getInstance(getActivity()).getAPIToken().isEmpty()) {
//            StaticMembers.openLogin(getActivity());
            StaticMembers.startActivityOverAll(getActivity(), LogInActivity.class);

        }
        noItem.setVisibility(View.GONE);
        Call<ProductsResponse> call = RetrofitModel.getApi(getActivity()).getWishList();
        call.enqueue(new CallbackRetrofit<ProductsResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<ProductsResponse> call, @NotNull Response<ProductsResponse> response) {
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

                swipe.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    maxPage = response.body().getData().getLastPage();
                    if (isRefresh || isRef)
                        productList.clear();
                    isRefresh = false;
                    productList.addAll(response.body().getData().getData());
                    adapter.notifyDataSetChanged();
                    if (productList.isEmpty())
                        noItem.setVisibility(View.VISIBLE);
                    else noItem.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(@NotNull Call<ProductsResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

                swipe.setRefreshing(false);
                noItem.setVisibility(View.GONE);
            }
        });
    }
}
