package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.User;

import java.util.Optional;
import java.util.List;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCognitoSub(String cognitoSub);

    List<User> findByRole(String role);

    boolean existsByEmail(String email);

    boolean existsByCognitoSub(String cognitoSub);

    long countByRole(String role);
}
