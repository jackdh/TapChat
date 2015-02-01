package uk.co.jackdh.tapchat;

import android.app.Application;
import com.parse.Parse;
import com.parse.ParseObject;

/**
 * Created by jack on 22/01/2015.
 */
public class TapchatApplication extends Application {

    @Override
    public void onCreate() {
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "07kNXDkfMCFsKr44vEacGr6chkd86lrTcrTtRaaH", "rTOgefpKKQeocxUm8griLlhmtD9NsHrkUClWLbf6");

    }
}
