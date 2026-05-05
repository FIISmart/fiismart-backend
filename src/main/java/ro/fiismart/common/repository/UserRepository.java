package ro.fiismart.common.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ro.fiismart.common.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCognitoSub(String cognitoSub);

    boolean existsByEmail(String email);

    boolean existsByCognitoSub(String cognitoSub);

    long countByRole(String role);
}
