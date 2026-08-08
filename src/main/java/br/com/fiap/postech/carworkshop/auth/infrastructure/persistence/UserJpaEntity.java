package br.com.fiap.postech.carworkshop.auth.infrastructure.persistence;

import br.com.fiap.postech.carworkshop.auth.domain.entity.UserRoles;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJpaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue
    public Long id;

    private String username;
    private String password;
    private String salt;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "userentity_roles", joinColumns = @JoinColumn(name = "userentity_id"))
    @Column(name = "roles")
    private List<UserRoles> roles = new ArrayList<>();
}
