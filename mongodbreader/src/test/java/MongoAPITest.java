import java.util.ArrayList;
import java.util.List;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.reader.mongodbreader.KeyConstant;
import com.alibaba.datax.plugin.reader.mongodbreader.util.MongoUtil;
import com.mongodb.*;

/**
 * Created by zhongliang.jia on 2015/09/08.
 */
public class MongoAPITest {
    //db.tag_data2.insert({"sid":"123","name":"steven"})
    public static void main(String[] args) {
       Configuration conf = Configuration.newDefault();
        List adds = new ArrayList();
        adds.add("100.69.195.221:33017");
        adds.add("100.69.198.134:33017");
        adds.add("100.69.202.74:33017");
        conf.set(KeyConstant.MONGO_ADDRESS,adds);
        conf.set(KeyConstant.MONGO_USER_NAME,"relitu");
        conf.set(KeyConstant.MONGO_USER_PASSWORD,"relitu&123");
        MongoClient mc = MongoUtil.initCredentialMongoClient(conf,"relitu","relitu&123","admin");
        String dbName = "admin";
        String collectionName = "foo";
        DBCollection dbcol = mc.getDB(dbName).getCollection(collectionName);
        DBObject insertObj = mockObj();
        System.out.println(dbcol.findOne());
        //dbcol.insert(insertObj);
       /* DBObject query = new BasicDBObject("unique_id","7341944476_660018890_20026351228");
        BulkWriteOperation bwo = dbcol.initializeUnorderedBulkOperation();
        DBObject data = mockData();
        bwo.find(query).upsert().replaceOne(data);
        bwo.execute();*/
    }

    private static DBObject mockObj() {
        DBObject obj = new BasicDBObject();
        obj.put("unique_id","7341944476_660018890_20026351228");
        obj.put("sid","7341944476");
        obj.put("user_id","660018890");
        obj.put("auction_id","20026351228");
        obj.put("content_type","1");
        obj.put("pool_type","0");
        obj.put("frontcat_id","6");
        obj.put("categoryid","21 50010099 50023023");
        obj.put("gmt_create","1417506");
        obj.put("taglist","防雨布 加厚 防水布 雨布 油布 布 帆布 军工 元 雨蓬 防雨 防水 6.8 雨蓬布 幅 居家日用 伞 雨具 防雨 防潮 防雨布");
        obj.put("property","-1");
        obj.put("scorea","107859");
        obj.put("scoreb","994");
        return obj;
    }

    private static DBObject mockData() {
        DBObject obj = new BasicDBObject();
        obj.put("unique_id","7341944476_660018890_20026351228");
        obj.put("sid","123454");
        obj.put("user_id","660018890");
        obj.put("auction_id","20026351228");
        obj.put("content_type","1");
        obj.put("pool_type","0");
        obj.put("frontcat_id","6");
        obj.put("categoryid","21 50010099 50023023");
        obj.put("gmt_create","1417506");
        obj.put("taglist","防雨布 加厚 防水布 雨布 油布 布 帆布 军工 元 雨蓬 防雨 防水 6.8 雨蓬布 幅 居家日用 伞 雨具 防雨 防潮 防雨布");
        obj.put("property","-1");
        obj.put("scorea","107859");
        obj.put("scoreb","994");
        return obj;
    }
}
