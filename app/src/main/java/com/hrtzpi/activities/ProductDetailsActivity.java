package com.hrtzpi.activities;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.viewpager.widget.ViewPager;

import com.google.gson.GsonBuilder;
import com.hrtzpi.R;
import com.hrtzpi.adapters.ProductImagesAdapter;
import com.hrtzpi.baseactivity.BaseActivity;
import com.hrtzpi.fragments.ImageFragment;
import com.hrtzpi.fragments.VideoFragment;
import com.hrtzpi.helpers.CallbackRetrofit;
import com.hrtzpi.helpers.Loading;
import com.hrtzpi.helpers.PrefManager;
import com.hrtzpi.helpers.RetrofitModel;
import com.hrtzpi.helpers.StaticMembers;
import com.hrtzpi.models.cart.AddCartResponse;
import com.hrtzpi.models.search_products.ProDetails;
import com.hrtzpi.models.search_products.Product;
import com.hrtzpi.models.wishlist_models.ErrorWishListResponse;
import com.hrtzpi.models.wishlist_models.WishlistResponse;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

public class ProductDetailsActivity extends BaseActivity {

    @BindView(R.id.pager)
    ViewPager pager;
    @BindView(R.id.addToCart)
    CardView addToCart;
    @BindView(R.id.addToCartText)
    TextView addToCartText;
    @BindView(R.id.indicator)
    LinearLayout indicator;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.reviewNo)
    TextView reviewNo;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.priceOld)
    TextView priceOld;
    @BindView(R.id.description)
    TextView description;
    @BindView(R.id.subCode)
    TextView subCode;
    @BindView(R.id.weight)
    TextView weight;
    @BindView(R.id.age)
    TextView age;
    @BindView(R.id.gender)
    TextView gender;
    @BindView(R.id.ratingBar)
    RatingBar ratingBar;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    int amount = 1, maxAmount = 1;
    Product product;
    ProductImagesAdapter adapter;
    VideoFragment videoFragment;
    ProDetails proDetails;
    @BindView(R.id.remove)
    ImageButton remove;
    @BindView(R.id.amount)
    TextView amounttv;
    @BindView(R.id.add)
    ImageButton add;
    private MenuItem favorite;
    Loading loading;
    String actualPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_details);
        ButterKnife.bind(this);
        loading = new Loading(this);
        product = (Product) getIntent().getSerializableExtra(StaticMembers.PRODUCT);
        adapter = new ProductImagesAdapter(getSupportFragmentManager());
        if (product.getVideo() != null) {
            videoFragment = VideoFragment.getInstance(product.getVideo());
            adapter.addFragment(videoFragment);
        }
        for (String s : product.getPhotos()) {
            ImageFragment imageFragment = ImageFragment.getInstance(s, product.getPhotos(), pager, this);
            adapter.addFragment(imageFragment);
        }
        pager.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        name.setText(product.getName());
        toolbar.setTitle(product.getName());
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        description.setText(product.getDetails());

        ratingBar.setRating(product.getRate());
        reviewNo.setText(String.format(Locale.getDefault(), getString(R.string.d_reviewers), 0));
        StaticMembers.changeDots(0, adapter.getCount(), indicator, getBaseContext());
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                StaticMembers.changeDots(position, adapter.getCount(), indicator, getBaseContext());
                if (videoFragment != null)
                    if (position != 0)
                        videoFragment.pauseVideo();
                    else videoFragment.resumeVideo();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        priceOld.setText(product.getPrice() + " " + getString(R.string.s_kwd));
        priceOld.setPaintFlags(priceOld.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        price.setText(product.getPrice() + " " + getString(R.string.s_kwd));
        actualPrice=product.getPrice()+"";

        if (product.getPrice() != null
                && !product.getNewPrice().isEmpty()
                && !product.getNewPrice().equals("0")) {
            price.setText(product.getNewPrice() + " " + getString(R.string.s_kwd));
            actualPrice=product.getNewPrice()+"";
            priceOld.setVisibility(View.VISIBLE);
        } else priceOld.setVisibility(View.GONE);
//        addToCartText.setText(String.format(Locale.getDefault(), getString(R.string.add_to_cart_s), Float.parseFloat(product.getPrice()) * amount));

        add.setOnClickListener(v ->
        {
            amount++;
            amounttv.setText(String.format(Locale.getDefault(), "%d", amount));
            if (amount > 1){
                remove.setEnabled(true);

            }
//            if (product.getProDetails().get(0).getCount() != null)
//                if (amount >= maxAmount)
//                    add.setEnabled(false);
            if (product.getPrice() != null && !actualPrice.isEmpty()) {
                float total = amount * Float.parseFloat(actualPrice);
//                addToCartText.setText(getString(R.string.add_to_cart_s)+" "+ total);
                addToCartText.setText(getString(R.string.add_to_cart_s) + " (" + total + ")");

            }
        });
        remove.setOnClickListener(v ->
        {
            if (amount < 2)
                remove.setEnabled(false);
            else {
                amount--;
                amounttv.setText(String.format(Locale.getDefault(), "%d", amount));
                if (amount < 2){
                    remove.setEnabled(false);
                }
//                if (product.getProDetails().get(0).getCount() != null)
//                    if (amount < maxAmount)
//                        add.setEnabled(true);
                if (actualPrice != null && !actualPrice.isEmpty()) {
                    float total = amount * Float.parseFloat(actualPrice);
//                    addToCartText.setText(getString(R.string.add_to_cart_s)+" "+total);
                    addToCartText.setText(getString(R.string.add_to_cart_s) + " (" + total + ")");

                }
            }
        });



        addToCartText.setText(getString(R.string.add_to_cart_s) + " (" + Float.parseFloat(product.getPrice()) * amount + ")");
        remove.setEnabled(false);

        subCode.setText(product.getProductNo());
        gender.setText(product.getGender() == 0 ? R.string.boy : R.string.girl);
        weight.setText(product.getWeight());
        addToCart.setOnClickListener(v -> changeCartItem());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.product_menu, menu);
        favorite = menu.findItem(R.id.favorite);
        favorite.setChecked(product.isWishlist());
        favorite.setIcon(favorite.isChecked() ? R.drawable.fav_red : R.drawable.fav_grey);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favorite:
                item.setChecked(!item.isChecked());
                item.setIcon(item.isChecked() ? R.drawable.fav_red : R.drawable.fav_grey);
                changeFavorite();
                break;
        }
        return true;
    }


    private void changeCartItem() {
        // progress.setVisibility(View.VISIBLE);
        if (loading != null) {
            loading.show();

        }

        if (PrefManager.getInstance(getBaseContext()).getAPIToken().isEmpty()) {
            if (loading != null && loading.isShowing()) {
                loading.dismiss();
            }
            Intent intent = new Intent(getBaseContext(), LogInActivity.class);
            intent.putExtra(StaticMembers.ACTION, true);
            startActivity(intent);
        } else {

            Call<AddCartResponse> call = RetrofitModel.getApi(this).addOrEditCart("" + product.getId(), Integer.parseInt(amounttv.getText().toString()));
            call.enqueue(new CallbackRetrofit<AddCartResponse>(this) {
                @Override
                public void onResponse(@NotNull Call<AddCartResponse> call, @NotNull Response<AddCartResponse> response) {
                    if (loading != null && loading.isShowing()) {
                        loading.dismiss();

                    }
                    if (!response.isSuccessful()) {
                        //amountText.setText(String.format(Locale.getDefault(), "%d", amount - 1));
                        StaticMembers.checkLoginRequired(response.errorBody(), getBaseContext(), ProductDetailsActivity.this);
                    } else if (response.body() != null) {
//                        startActivity(new Intent(getBaseContext(), CartActivity.class));
                        StaticMembers.toastMessageShortSuccess(getBaseContext(), response.body().getMessage());
                        //amountText.setText(String.format(Locale.getDefault(), "%d", amount));
                    }
                }

                @Override
                public void onFailure(@NotNull Call<AddCartResponse> call, @NotNull Throwable t) {
                    super.onFailure(call, t);
                    if (loading != null && loading.isShowing()) {
                        loading.dismiss();

                    }
                }
            });
        }
    }

    private void changeFavorite() {
        //progress.setVisibility(View.VISIBLE);
        if (loading != null) {
            loading.show();

        }
        if (PrefManager.getInstance(getBaseContext()).getAPIToken().isEmpty()) {
            if (loading != null && loading.isShowing()) {
                loading.dismiss();
            }
            StaticMembers.openLogin(this);
        } else {
            Call<WishlistResponse> call = RetrofitModel.getApi(this).toggleWishlist(product.getId());
            call.enqueue(new CallbackRetrofit<WishlistResponse>(this) {
                @Override
                public void onResponse(@NotNull Call<WishlistResponse> call, @NotNull Response<WishlistResponse> response) {
                    if (loading != null && loading.isShowing()) {
                        loading.dismiss();

                    }
                    WishlistResponse result = response.body();
                    if (response.isSuccessful() && result != null) {
                        StaticMembers.toastMessageShortSuccess(getBaseContext(), result.getMessage());
                    } else {
                        favorite.setChecked(!favorite.isChecked());
                        favorite.setIcon(favorite.isChecked() ? R.drawable.fav_red : R.drawable.fav_grey);
                        try {
                            ErrorWishListResponse errorLoginResponse = null;
                            if (response.errorBody() != null) {
                                errorLoginResponse = new GsonBuilder().create().fromJson(response.errorBody().string(), ErrorWishListResponse.class);
                                if (errorLoginResponse != null) {
                                    StaticMembers.toastMessageShortFailed(getBaseContext(), errorLoginResponse.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        StaticMembers.checkLoginRequired(response.errorBody(), ProductDetailsActivity.this, ProductDetailsActivity.this);

                    }
                }

                @Override
                public void onFailure(@NotNull Call<WishlistResponse> call, @NotNull Throwable t) {
                    super.onFailure(call, t);
                    if (loading != null && loading.isShowing()) {
                        loading.dismiss();

                    }
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
