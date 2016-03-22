package com.cleveroad.splittransformation.example;

import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Single ViewPager's item fragment.
 */
public class PagerFragment extends Fragment {

    private static final String KEY_DRAWABLE = "DRAWABLE";
    private static final String KEY_TEXT = "TEXT";

    private TextView textView;

    public static PagerFragment instance(@DrawableRes int drawableId) {
        Bundle args = new Bundle();
        args.putInt(KEY_DRAWABLE, drawableId);
        PagerFragment fragment = new PagerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.pager_item_et, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.image);
        imageView.setImageDrawable(getContext().getResources().getDrawable(getArguments().getInt(KEY_DRAWABLE)));
        textView = (TextView) view.findViewById(R.id.text);
        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_TEXT, textView.getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            textView.setText(savedInstanceState.getString(KEY_TEXT));
        }
    }
}
