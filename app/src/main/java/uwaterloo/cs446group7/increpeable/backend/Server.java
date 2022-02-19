package uwaterloo.cs446group7.increpeable.backend;
import android.util.Pair;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.FutureTask;

import io.realm.RealmObject;
import uwaterloo.cs446group7.increpeable.objects.User;

public class Server {
    private RealmClient rmc;

    public Server(RealmClient rmc) {
        this.rmc = rmc;
    }
    public boolean checkUsername(String name) {
        List<Pair<String, String>> criteria = new ArrayList<>();
        criteria.add(new Pair<>("username", name));
        RealmClient.TransactionResult<User> tr = rmc.readTrans(criteria, User.class);
        return tr.results.size() == 0;
    }
    public boolean createNewAccount(User newUser) {
        if (!checkUsername(newUser.getName())) return false;
        return rmc.createTrans(newUser);
    }
}
