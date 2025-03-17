package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

public class Contact {
    // 用于缓存联系人姓名，键是电话号码，值是姓名
    private static HashMap<String, String> sContactCache;
    private static final String TAG = "Contact";

    // 查询联系人的 SQL 语句模板。
    // PHONE_NUMBERS_EQUAL 是一个函数，用于比较两个电话号码是否相等（忽略格式差异）。
    // Data.MIMETYPE 用于筛选出电话号码类型的数据。
    // Data.RAW_CONTACT_ID 用于连接到 phone_lookup 表。
    // phone_lookup 表用于根据最小匹配（min_match）查找联系人。
    // '+' 会被替换为实际的最小匹配电话号码。
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
    + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
    + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    /**
     * 根据电话号码获取联系人姓名。
     *
     * @param context     上下文对象
     * @param phoneNumber 电话号码
     * @return 联系人姓名，如果找不到则返回 null
     */
    public static String getContact(Context context, String phoneNumber) {
        // 如果缓存为空，则初始化缓存
        if(sContactCache == null) {
            sContactCache = new HashMap<String, String>();
        }
        // 将 SQL 语句模板中的 '+' 替换为最小匹配的电话号码
        if(sContactCache.containsKey(phoneNumber)) {
            return sContactCache.get(phoneNumber);
        }

        // 如果缓存中已存在该号码，则直接返回缓存的姓名
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String [] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String name = cursor.getString(0);
                sContactCache.put(phoneNumber, name);
                return name;
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, " Cursor get string error " + e.toString());
                return null;
            } finally {
                cursor.close();
            }
        } else {
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}
