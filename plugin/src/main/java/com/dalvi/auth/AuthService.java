package com.dalvi.auth;

import com.dalvi.models.AuthCode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private static final int AUTH_CODE_EXPIRATION = 300;
    private final Map<UUID, AuthCode> authCodes = new HashMap<>();

    public String generateAuthCode(UUID playerId) {
        String code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        authCodes.put(playerId, new AuthCode(code, System.currentTimeMillis() + AUTH_CODE_EXPIRATION * 1000));
        return code;
    }

    public UUID getPlayerIdFromAuthCode(String code) {
        for (Map.Entry<UUID, AuthCode> entry : authCodes.entrySet()) {
            AuthCode authCode = entry.getValue();
            if (authCode.getCode().equals(code) && System.currentTimeMillis() <= authCode.getExpiration()) {
                this.removeAuthCode(entry.getKey());
                return entry.getKey();
            }
        }
        return null;
    }

    public void removeAuthCode(UUID playerId) {
        authCodes.remove(playerId);
    }
}
