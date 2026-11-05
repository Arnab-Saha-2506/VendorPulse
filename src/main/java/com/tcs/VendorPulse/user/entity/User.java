package com.tcs.VendorPulse.user.entity;
import com.tcs.VendorPulse.common.enums.Role;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @Column(unique=true, nullable=false)
    private String email;

    @Column(nullable=false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private Role role;

    @Column(nullable=false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate(){
        createdAt=LocalDateTime.now();

    }
    public Long getId(){
        return id;
    }
    public void setId(Long id){
        this.id=id;
    }
    public String getEmail(){
        return email;
    }
    public void setEmail(String e){
        this.email=e;
    }
    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String p){
        this.passwordHash=p;
    }
    public Role getRole(){
        return role;
    }
    public void setRole(Role r){
        this.role=r;
    }
}