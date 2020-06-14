package com.hrtzpi.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.hrtzpi.R;
import com.hrtzpi.activities.CartActivity;
import com.hrtzpi.activities.LogInActivity;
import com.hrtzpi.adapters.AdditionsAdapter;
import com.hrtzpi.adapters.CoversAdapter;
import com.hrtzpi.helpers.CallbackRetrofit;
import com.hrtzpi.helpers.Loading;
import com.hrtzpi.helpers.PrefManager;
import com.hrtzpi.helpers.RetrofitModel;
import com.hrtzpi.helpers.StaticMembers;
import com.hrtzpi.models.cart.AddCartResponse;
import com.hrtzpi.models.cart.CartItem;
import com.hrtzpi.models.cart.GiftAdditionalItem;
import com.hrtzpi.models.gifts.additional.AdditionalResponse;
import com.hrtzpi.models.gifts.covers.CoverResponse;
import com.hrtzpi.models.gifts.covers.DataItem;
import com.hrtzpi.models.gifts.messages.MessageInput;
import com.wang.avi.AVLoadingIndicatorView;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Response;

public class GiftsFragment extends DialogFragment {


    private CartItem product;
    @BindView(R.id.image)
    ImageView image;
    @BindView(R.id.name)
    TextView name;
    @BindView(R.id.productId)
    TextView productId;
    @BindView(R.id.chooseCoverText)
    TextView chooseCoverText;
    @BindView(R.id.chooseAddText)
    TextView chooseAddText;
    @BindView(R.id.price)
    TextView price;
    @BindView(R.id.messageText)
    TextView messageText;
    @BindView(R.id.covers)
    RecyclerView coversRecycler;
    @BindView(R.id.additionals)
    RecyclerView additionalsRecycler;
    @BindView(R.id.save)
    CardView saveButton;
    private List<DataItem> covers;
    private CoversAdapter coversAdapter;
    private List<com.hrtzpi.models.gifts.additional.DataItem> additionals;
    private AdditionsAdapter additionsAdapter;
    private MessageInput messageInput;
    private HashMap<String, String> params;
    Loading loading;

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        loading=new Loading(getActivity());
        if (savedInstanceState != null)
            product = (CartItem) savedInstanceState.getSerializable(StaticMembers.PRODUCT);
        if (product != null) {
            if (product.getProduct() != null)
                Glide.with(Objects.requireNonNull(getActivity()))
                        .asBitmap()
                        .apply(new RequestOptions().override(75, 75))
                        .load(product.getProduct().getLogo())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(image);
//                Glide.with(Objects.requireNonNull(getActivity())).load(product.getProduct().getLogo()).into(image);
            name.setText(product.getProduct() != null ? product.getProduct().getName() : "");
            productId.setText(getString(R.string.product_id_s)+" "+ product.getProductId());
            price.setText(product.getPrice()+" "+getString(R.string.s_kwd));
            params = new HashMap<>();
            params.put(StaticMembers.PRODUCT_ID, "" + product.getProduct().getId());
            params.put(StaticMembers.QUANTITY, product.getQuantity());

            if (messageInput == null){
                messageInput = new MessageInput();

                messageInput.setCoverId(product.getGiftMessage() == null ? -1 : product.getGiftMessage().getId());
                if (product.getGiftMessegsFrom()!=null){
                    messageInput.setFrom(product.getGiftMessegsFrom());

                }
                if (product.getGiftMessegsTo()!=null){
                    messageInput.setTo(product.getGiftMessegsTo());
                }
                if (product.getGiftMessegsMsg()!=null){
                    messageInput.setMessage(product.getGiftMessegsMsg());
                }

                changeMessageText(messageInput);

            }

        }
        messageText.setOnClickListener(v -> {
            //TODO: Message Bottom Sheet Dialog
            MessageBottomSheet.getInstance(messageInput, this::changeMessageText)
                    .show(Objects.requireNonNull(getActivity()).getSupportFragmentManager(), "Message");
        });
        covers = new ArrayList<>();
        additionals = new ArrayList<>();
        coversAdapter = new CoversAdapter(getContext(), covers, pos -> {
            if (pos < covers.size() && pos > -1) {
                params.put(StaticMembers.GIFT_COVERS_ID, "" + covers.get(pos).getId());
                float total = Float.parseFloat(covers.get(pos).getPrice());
                chooseCoverText.setText(String.format(Locale.getDefault(),
                        getString(R.string.choose_from_covers), new DecimalFormat("#.#").format(total)));
            } else {
                chooseCoverText.setText(String.format(Locale.getDefault(),
                        getString(R.string.choose_from_covers), new DecimalFormat("#.#").format(0)));
                params.remove(StaticMembers.GIFT_COVERS_ID);
            }
        });
        additionsAdapter = new AdditionsAdapter(getContext(), additionals, pos -> {
            float total = 0;
            for (com.hrtzpi.models.gifts.additional.DataItem add : additionals) {
                if (add.isSelected()) {
                    total += Float.parseFloat(add.getPrice()) * add.getAmount();
                }
            }
            chooseAddText.setText(String.format(Locale.getDefault(), getString(R.string.choose_from_additionals), new DecimalFormat("#.#").format(total)));

        });
        coversRecycler.setAdapter(coversAdapter);
        additionalsRecycler.setAdapter(additionsAdapter);
        getCovers();
        getAdditions();
        chooseCoverText.setText(String.format(Locale.getDefault(), getString(R.string.choose_from_covers), new DecimalFormat("#.#").format(0)));
        chooseAddText.setText(String.format(Locale.getDefault(), getString(R.string.choose_from_additionals), new DecimalFormat("#.#").format(0)));

        saveButton.setOnClickListener(v ->
        {
            //TODO: Save changes as a cart new product
                changeCartItem();

        });

    }

    private void changeMessageText(MessageInput messageInput1) {
        StringBuilder stringBuilder = new StringBuilder();
        if (messageInput1.getFrom() != null && !messageInput1.getFrom().isEmpty())
            stringBuilder.append(String.format(Locale.getDefault(), getString(R.string.from_s), messageInput1.getFrom())).append("\n");
        if (messageInput1.getTo() != null && !messageInput1.getTo().isEmpty())
            stringBuilder.append(String.format(Locale.getDefault(), getString(R.string.to_s), messageInput1.getTo())).append("\n");
        if (messageInput1.getMessage() != null && !messageInput1.getMessage().isEmpty())
            stringBuilder.append(String.format(Locale.getDefault(), getString(R.string.message_s), messageInput1.getMessage())).append("\n");
        if (messageInput1.getCoverId() != -1) {
//            stringBuilder.append(getString(R.string.cover_id)+" "+messageInput1.getCoverId()).append("\n");
            params.put(StaticMembers.GIFT_MESSAGE_ID, "" + messageInput1.getCoverId());
        } else params.remove(StaticMembers.GIFT_MESSAGE_ID);
        messageText.setText(stringBuilder);
        params.put(StaticMembers.GIFT_MESSAGE_FROM, messageInput1.getFrom()+"");
        params.put(StaticMembers.GIFT_MESSAGE_TO, messageInput1.getTo()+"");
        params.put(StaticMembers.GIFT_MESSAGE_TEXT, messageInput1.getMessage()+"");
    }


    private void changeCartItem() {

        if (loading!=null){
            loading.show();

        }
        if (PrefManager.getInstance(getContext()).getAPIToken().isEmpty()) {
            Intent intent = new Intent(getContext(), LogInActivity.class);
            intent.putExtra(StaticMembers.ACTION, true);
            startActivity(intent);
        } else {
            List<String> addIDs = new ArrayList<>();
            List<String> addQuantities = new ArrayList<>();
            for (com.hrtzpi.models.gifts.additional.DataItem addition : additionals) {
                if (addition.isSelected()) {
                    addIDs.add("" + addition.getId());
                    addQuantities.add("" + addition.getAmount());
                }
            }
            Call<AddCartResponse> call = RetrofitModel.getApi(getActivity())
                    .addOrEditCart(params, addIDs, addQuantities);
            call.enqueue(new CallbackRetrofit<AddCartResponse>(getActivity()) {
                @Override
                public void onResponse(@NotNull Call<AddCartResponse> call, @NotNull Response<AddCartResponse> response) {
                    if (loading!=null&&loading.isShowing()){
                        loading.dismiss();
                    }

                    if (!response.isSuccessful()) {
                        //amountText.setText(String.format(Locale.getDefault(), "%d", amount - 1));
                        StaticMembers.checkLoginRequired(response.errorBody(), getContext(),getActivity());
                    } else if (response.body() != null) {
                       /* startActivity(new Intent(getBaseContext(), CartActivity.class));
                        StaticMembers.toastMessageShort(getBaseContext(), response.body().getMessage());*/
                        //amountText.setText(String.format(Locale.getDefault(), "%d", amount));
                        ((CartActivity) Objects.requireNonNull(getActivity())).getCart();
                        dismiss();
                    }
                }

                @Override
                public void onFailure(@NotNull Call<AddCartResponse> call, @NotNull Throwable t) {
                    super.onFailure(call, t);
                    if (loading!=null&&loading.isShowing()){
                        loading.dismiss();

                    }

                }
            });
        }
    }

    void getCovers() {

        if (loading!=null){
            loading.show();

        }

        Call<CoverResponse> call = RetrofitModel.getApi(getActivity()).getCovers();
        call.enqueue(new CallbackRetrofit<CoverResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<CoverResponse> call, @NotNull Response<CoverResponse> response) {
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    covers.clear();
                    covers.addAll(response.body().getData());
                    for (int i = 0; i < covers.size(); i++) {
                        if (product != null) {
                            DataItem cover = covers.get(i);
                            if (product.getGiftCover() != null) {
                                if (product.getGiftCover().getId() == cover.getId()) {
                                    coversAdapter.setCheckedIndex(i);
                                    params.put(StaticMembers.GIFT_COVERS_ID, "" + cover.getId());
                                    chooseCoverText.setText(String.format(Locale.getDefault(),
                                            getString(R.string.choose_from_covers),
                                            new DecimalFormat("#.#").format(Float.parseFloat(cover.getPrice()))));
                                    break;
                                }
                            } else break;
                        } else break;
                    }
                    coversAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NotNull Call<CoverResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }
            }
        });

    }

    void getAdditions() {

        if (loading!=null){
            loading.show();

        }
        Call<AdditionalResponse> call = RetrofitModel.getApi(getActivity()).getAdditions();
        call.enqueue(new CallbackRetrofit<AdditionalResponse>(getActivity()) {
            @Override
            public void onResponse(@NotNull Call<AdditionalResponse> call, @NotNull Response<AdditionalResponse> response) {
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

                              if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    additionals.clear();
                    additionals.addAll(response.body().getData());
                    float total = 0;
                    for (int i = 0; i < additionals.size(); i++) {
                        if (product != null && (product.getGiftAdditional() != null)) {
                            com.hrtzpi.models.gifts.additional.DataItem additional = additionals.get(i);
                            for (GiftAdditionalItem additionalItem : product.getGiftAdditional()) {
                                if (additionalItem.getAdditionalId() == additional.getId()) {
                                    additional.setSelected(true);
                                    additional.setQuantity(additionalItem.getQuantity());
                                    total += Float.parseFloat(additional.getPrice()) * additional.getAmount();
                                }
                            }
                        } else break;
                    }
                    chooseAddText.setText(String.format(Locale.getDefault(), getString(R.string.choose_from_additionals), new DecimalFormat("#.#").format(total)));
                    additionsAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NotNull Call<AdditionalResponse> call, @NotNull Throwable t) {
                super.onFailure(call, t);
                if (loading!=null&&loading.isShowing()){
                    loading.dismiss();

                }

            }
        });

    }

    public static GiftsFragment getInstance(CartItem product) {
        GiftsFragment fragment = new GiftsFragment();
        fragment.product = product;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AppTheme);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(StaticMembers.PRODUCT, product);
    }

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_gifts, container, false);
        Toolbar actionBar = v.findViewById(R.id.toolbar);
        if (actionBar != null) {
            actionBar.setNavigationOnClickListener(v1 -> getDialog().dismiss());
        }
        return v;
    }
}
