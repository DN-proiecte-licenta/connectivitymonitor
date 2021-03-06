package com.a45g.athena.connectivitymonitor;


import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.a45g.athena.connectivitymonitor.HelperFunctions.getTime;
import static com.a45g.athena.connectivitymonitor.HelperFunctions.sudoForResult;

public class TestFragment extends Fragment {

    private static final String LOG_TAG = TestFragment.class.getName();

    private TextView mScriptText = null;
    private EditText mScriptName = null;
    private TextView mDomainText = null;
    private EditText mDomainName = null;
    private TextView mPortText = null;
    private EditText mPortValue = null;
    private TextView mTimesText = null;
    private EditText mTimesValue = null;

    private TextView mResultText = null;


    private Button mStart = null;
    private TextView mResult = null;
    private LinearLayout mLayout = null;
    private View mScrollView = null;

    private int mScrollPos;
    private int mMaxScrollPosition;
    private Runnable checkScrollRunnable = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.test_fragment, container, false);

        mScriptText = (TextView) rootView.findViewById(R.id.scriptText);
        mScriptName = (EditText) rootView.findViewById(R.id.scriptName);

        mDomainText = (TextView) rootView.findViewById(R.id.domainText);
        mDomainName = (EditText) rootView.findViewById(R.id.domainName);

        mPortText = (TextView) rootView.findViewById(R.id.portText);
        mPortValue = (EditText) rootView.findViewById(R.id.portValue);

        mTimesText = (TextView) rootView.findViewById(R.id.timesText);
        mTimesValue = (EditText) rootView.findViewById(R.id.timesValue);

        mResultText = (TextView) rootView.findViewById(R.id.resultText);


        mStart = (Button) rootView.findViewById(R.id.start);
        mResult = (TextView) rootView.findViewById(R.id.testResult);
        mLayout = (LinearLayout) rootView.findViewById(R.id.testLayout);
        mScrollView = (ScrollView) rootView.findViewById(R.id.testScrollView);

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "test2");

                String times = mTimesValue.getText().toString();

                mResult.setText("");

                for (int i = 0; i < Integer.valueOf(times); i++) {
                    new ExecuteTask().execute(
                            "/data/user/0/org.qpython.qpy/files/bin/qpython-android5.sh " +
                                    mScriptName.getText() + " " + mDomainName.getText() + " " +
                                    mPortValue.getText() + " && exit");
                }
            }
        });

        mResult.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                // This should not happen.
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
                mScrollPos = mScrollView.getScrollY();
                mMaxScrollPosition = mLayout.getHeight() - mScrollView.getHeight();
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                checkScroll(mLayout.getHeight() - mScrollView.getHeight() - mMaxScrollPosition);
            }

        });

        checkScrollRunnable = new Runnable() {
            @Override
            public void run() {
                ((ScrollView) mScrollView).fullScroll(View.FOCUS_DOWN);
            }
        };

        //this.setHasOptionsMenu(true);
        return rootView;
    }

    private void checkScroll(int added) {
        if (mScrollPos == 0) {
            mScrollView.post(checkScrollRunnable);
        } else {
            Log.d(LOG_TAG, "Scrolling with addition: " + (-added));
            mScrollView.scrollBy(0, -added);
        }
    }

    private class ExecuteTask extends AsyncTask<String, Integer, String> {

        protected String doInBackground(String... name) {

            String output = sudoForResult(name[0]);
            String[] lineTokens = output.split("\n");
            String[] tokens = lineTokens[0].split(" ");

            if (tokens.length >= 5)
                return tokens[4];
            else
                return null;
        }

        protected void onPostExecute(String result) {

            if (result == null) return;

            DatabaseOperations databaseOperations = new DatabaseOperations(getContext());
            databaseOperations.openWrite();
            databaseOperations.insertTestResult(getTime(), "RTT", result);
            databaseOperations.close();

            CharSequence previousResults = mResult.getText();
            mResult.setText(previousResults + "\n" + result + " ms");
            Log.d(LOG_TAG, result);
        }
    }


}
