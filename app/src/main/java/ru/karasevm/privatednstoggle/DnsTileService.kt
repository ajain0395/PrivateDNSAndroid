package ru.karasevm.privatednstoggle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import ru.karasevm.privatednstoggle.utils.PreferenceHelper
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.autoMode
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.dns_servers

const val DNS_MODE_OFF = "off"
const val DNS_MODE_AUTO = "opportunistic"
const val DNS_MODE_PRIVATE = "hostname"

class DnsTileService : TileService() {


    private fun checkForPermission(): Boolean {
        if (checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        Toast.makeText(this, R.string.permission_missing, Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onTileAdded() {
        super.onTileAdded()
        checkForPermission()
        // Update state
        qsTile.state = Tile.STATE_INACTIVE

        // Update looks
        qsTile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (!checkForPermission()) {
            return
        }

        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")
        val dnsProvider = Settings.Global.getString(contentResolver, "private_dns_specifier")

        val sharedPrefs = PreferenceHelper.defaultPreference(this)

        if (dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            if (sharedPrefs.autoMode) {
                changeTileState(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    getString(R.string.dns_auto),
                    R.drawable.ic_auto_black_24dp,
                    DNS_MODE_AUTO,
                    dnsProvider
                )
            } else {
                changeTileState(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    getNextAddress(dnsProvider),
                    R.drawable.ic_private_black_24dp,
                    DNS_MODE_PRIVATE,
                    getNextAddress(dnsProvider)
                )

            }

        } else if (dnsMode == null || dnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            changeTileState(
                qsTile,
                Tile.STATE_ACTIVE,
                getNextAddress(dnsProvider),
                R.drawable.ic_private_black_24dp,
                DNS_MODE_PRIVATE,
                getNextAddress(dnsProvider)
            )
        } else if (dnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            if (getNextAddress(dnsProvider) == null) {
                changeTileState(
                    qsTile,
                    Tile.STATE_INACTIVE,
                    getString(R.string.dns_off),
                    R.drawable.ic_off_black_24dp,
                    DNS_MODE_OFF,
                    getNextAddress(dnsProvider)
                )
            } else {
                changeTileState(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    getNextAddress(dnsProvider),
                    R.drawable.ic_private_black_24dp,
                    DNS_MODE_PRIVATE,
                    getNextAddress(dnsProvider)
                )
            }
        }

    }

    override fun onStartListening() {
        super.onStartListening()
        if (!checkForPermission()) {
            return
        }
        val dnsMode = Settings.Global.getString(contentResolver, "private_dns_mode")

        // Prevent some crashes
        if (qsTile == null) {
            return
        }

        if (dnsMode.equals(DNS_MODE_OFF, ignoreCase = true)) {
            refreshTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_off),
                R.drawable.ic_off_black_24dp
            )
        } else if (dnsMode == null) {
            refreshTile(
                qsTile,
                Tile.STATE_INACTIVE,
                getString(R.string.dns_unknown),
                R.drawable.ic_unknown_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_AUTO, ignoreCase = true)) {
            refreshTile(
                qsTile,
                Tile.STATE_ACTIVE,
                getString(R.string.dns_auto),
                R.drawable.ic_auto_black_24dp
            )
        } else if (dnsMode.equals(DNS_MODE_PRIVATE, ignoreCase = true)) {
            val dnsProvider = Settings.Global.getString(contentResolver, "private_dns_specifier")
            if (dnsProvider != null) {
                refreshTile(
                    qsTile,
                    Tile.STATE_ACTIVE,
                    dnsProvider,
                    R.drawable.ic_private_black_24dp
                )
            } else {
                Toast.makeText(this, R.string.permission_missing, Toast.LENGTH_SHORT).show()
            }
        }

    }

    /**
     * Updates tile  to specified parameters
     *
     * @param tile tile to update
     * @param state tile state
     * @param label tile label
     * @param icon tile icon
     */
    private fun refreshTile(tile: Tile, state: Int, label: String?, icon: Int) {
        tile.state = state
        tile.label = label
        tile.icon = Icon.createWithResource(this, icon)
        tile.updateTile()
    }

    /**
     * Updates tile and system settings to specified parameters
     *
     * @param tile tile to update
     * @param state tile state
     * @param label tile label
     * @param icon tile icon
     * @param dnsMode system dns mode
     * @param dnsProvider system dns provider
     */
    private fun changeTileState(
        tile: Tile,
        state: Int,
        label: String?,
        icon: Int,
        dnsMode: String,
        dnsProvider: String?
    ) {
        tile.label = label
        tile.state = state
        tile.icon = Icon.createWithResource(this, icon)
        Settings.Global.putString(contentResolver, "private_dns_mode", dnsMode)
        Settings.Global.putString(contentResolver, "private_dns_specifier", dnsProvider)
        tile.updateTile()
    }

    /**
     * Gets next dns address from preferences,
     * if current address is last returns null
     *
     * @param currentAddress currently set address
     * @return next address
     */
    private fun getNextAddress(currentAddress: String?): String? {
        val sharedPrefs = PreferenceHelper.defaultPreference(this)
        val items = sharedPrefs.dns_servers

        // Fallback if list is empty
        if (items[0] == "") {
            items.removeAt(0)
            items.add("dns.google")
        }

        val index = items.indexOf(currentAddress)

        if (index == -1 || currentAddress == null) {
                return items[0]
        }
        if (index == items.size - 1) {
            return null
        }
        return items[index + 1]
    }
}