package com.example.maxim;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class PermissionsUtility {
    private static final int PERMISSION_REQUEST_CODE = 1;

    public static void requestPermissions(Activity activity, String permission, Context context) {
        if (checkPermission(context,permission)) return;

        ArrayList<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(android.Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        }
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);

        ActivityCompat.requestPermissions(
                activity,
                permissions.toArray(new String[0]),
                PERMISSION_REQUEST_CODE
        );
    }




    public static boolean checkPermission(Context context, String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPermission(permission,context) &&
                    hasPermission(permission,context) &&
                    hasPermission(permission,context);
        }
        return hasPermission(permission,context);
    }

    public static boolean hasPermission(String permissionType, Context context) {
        return ContextCompat.checkSelfPermission(context, permissionType)
                == PackageManager.PERMISSION_GRANTED;
    }
}
