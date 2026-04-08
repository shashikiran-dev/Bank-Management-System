package com.omkaar.bank.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // store hashed; plain-text here for simplicity

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "pin_hash")
    private String pinHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected UserEntity() {}

    public UserEntity(UUID id, String name, String email, String password,
                      UUID accountId, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.accountId = accountId;
        this.createdAt = createdAt;
    }

    public UUID getId()          { return id; }
    public String getName()      { return name; }
    public String getEmail()     { return email; }
    public String getPassword()  { return password; }
    public UUID getAccountId()   { return accountId; }
    public String getPinHash()   { return pinHash; }
    public Instant getCreatedAt(){ return createdAt; }

    public void setPassword(String password) { this.password = password; }
    public void setPinHash(String pinHash)   { this.pinHash = pinHash; }
}
