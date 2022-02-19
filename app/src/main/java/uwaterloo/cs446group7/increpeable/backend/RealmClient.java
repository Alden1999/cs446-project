package uwaterloo.cs446group7.increpeable.backend;

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.RealmQuery;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.sync.SyncConfiguration;
import java.util.concurrent.FutureTask;
import io.realm.RealmObject;

public class RealmClient {
    Realm realm;
    App app;
    // Constructor
    public RealmClient() {
        buildApp();
        authenticate();
    }
    // Destructor
    public void finalize() {
        // Close realm transactions
        realm.close();

        // Log out of Database
        app.currentUser().logOutAsync(result -> {
            if (result.isSuccess()) {
                Log.v("Backend", "MongoDB user successfully logged out.");
            } else {
                Log.e("Backend", "MongoDB user failed to log out, error: " + result.getError());
            }
        });
    }

    // Transaction result returned for readTrans; all other 3 transaction types simply return a boolean
    public class TransactionResult <T extends RealmObject> {
        public boolean isSuccess;
        public List<T> results;
        TransactionResult (boolean isSuccess) {this.isSuccess = isSuccess;}
    }

    // Transaction calls to the Database; called by business logic layer
    public <T extends RealmObject> boolean createTrans (T realmObject) {
        FutureTask<Object> task = new FutureTask(new CreateCallable<T>(realm, realmObject));
        boolean rv = runTask(task);
        return rv;
    }
    public <T extends RealmObject> TransactionResult readTrans (List<Pair<String, String>> criteria, Class<T> type) {
        FutureTask<Object> task = new FutureTask(new ReadCallable(realm, criteria, type));
        TransactionResult<T> result = runReadTask(task);
        return result;
    }
    public <T extends RealmObject> boolean updateTrans (T realmObject, String object_id, Class<T> type) {
        FutureTask<Object> task = new FutureTask(new UpdateCallable(realm, realmObject, object_id, type));
        boolean rv = runTask(task);
        return rv;
    }
    public <T extends RealmObject> boolean deleteTrans (String object_id, Class<T> type) {
        FutureTask<Object> task = new FutureTask(new DeleteCallable(realm, object_id, type));
        boolean rv = runTask(task);
        return rv;
    }

    // To build the MongoDB Realm App
    private void buildApp() {
        String appID = "increpeable-eixfd";
        app = new App(new AppConfiguration.Builder(appID)
                .build());
    }

    // Log in to MongoDB Realm App, to perform transactions
    private void authenticate() {
        // login
        Credentials credentials = Credentials.anonymous();
        app.loginAsync(credentials, result -> {
            if (result.isSuccess()) {
                Log.v("QUICKSTART", "Successfully authenticated anonymously.");
                User user = app.currentUser();
                String partitionValue = "partition1";
                SyncConfiguration config = new SyncConfiguration.Builder(
                        user,
                        partitionValue)
                        .build();
                realm = Realm.getInstance(config);
            } else {
                Log.e("QUICKSTART", "Failed to log in. Error: " + result.getError());
            }
        });
    }

    // Run an async task for create, update, delete
    private boolean runTask(FutureTask<Object> task) {
        Thread t = new Thread(task);
        t.start();
        try {
            task.get();
            Log.i("MongoDB", "Create transaction succeeds.");
            return true;
        } catch (Exception e) {
            Log.e("MongoDB", "Create transaction failed." + e.getMessage());
            return false;
        }
    }

    // Run an async task for read
    private <T extends RealmObject> TransactionResult runReadTask(FutureTask<Object> task) {
        Thread t = new Thread(task);
        t.start();
        TransactionResult<T> tr = new TransactionResult<T>(false);
        try {
            tr.results = new ArrayList<>((Collection<T>)task.get());
            tr.isSuccess = true;
            Log.i("MongoDB", "Create transaction succeeds.");
            return tr;
        } catch (Exception e) {
            Log.e("MongoDB", "Create transaction failed." + e.getMessage());
            return tr;
        }
    }

    private class CreateCallable <T extends RealmObject> implements Callable {
        T object;
        Realm realm;

        public CreateCallable(Realm realm, T object) {
            this.realm = realm;
            this.object = object;
        }
        @Override
        public Object call() {
            realm.executeTransaction (transactionRealm -> {
                transactionRealm.insert(object);
            });
            return true;
        }
    }

    private class ReadCallable <T extends RealmObject> implements Callable {
        Realm realm;
        // criteria format: array of <String type, String key, String value>
        List<Pair<String, String>> criteria;
        Class<T> type;
        public ReadCallable(Realm realm, List<Pair<String, String>> criteria, Class<T> type) {
            this.realm = realm;
            this.criteria = criteria;
            this.type = type;
        }

        @Override
        public Object call() {
            RealmQuery<T> results = realm.where(type);
            for (int i = 0; i < criteria.size(); i++) {
                results = results.equalTo(criteria.get(i).first, criteria.get(i).second);
            }
            return realm.copyFromRealm(results.findAll());
        }
    }

    private class UpdateCallable <T extends RealmObject> implements Callable {
        private Realm realm;
        private String object_id;
        private T object;
        private final Class<T> type;
        public UpdateCallable(Realm realm, T object, String object_id, Class<T> type) {
            this.realm = realm;
            this.object_id = object_id;
            this.object = object;
            this.type = type;
        }

        @Override
        public Object call() {
            realm.executeTransaction(transactionRealm -> {
                T objectFound = transactionRealm.where(type).equalTo("_id", object_id).findFirst();
                objectFound.deleteFromRealm();
                transactionRealm.insert(object);
            });
            return true;
        }
    }

    private class DeleteCallable <T extends RealmObject> implements Callable {
        Realm realm;
        String object_id;
        Class<T> type;
        public DeleteCallable(Realm realm, String object_id, Class<T> type) {
            this.realm = realm;
            this.object_id = object_id;
            this.type = type;
        }

        @Override
        public Object call() {
            realm.executeTransaction(transactionRealm -> {
                T objectFound = transactionRealm.where(type).equalTo("_id", object_id).findFirst();
                objectFound.deleteFromRealm();
            });
            return true;
        }
    }
}