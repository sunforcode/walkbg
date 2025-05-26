package org.example.service

import org.example.model.User
import org.example.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    fun getAllUsers(): List<User> = userRepository.findAll()
    
    fun getUserById(id: Long): User? = userRepository.findById(id).orElse(null)
    
    fun getUserByEmail(email: String): User? = userRepository.findByEmail(email)
    
    @Transactional
    fun createUser(user: User): User = userRepository.save(user)
    
    @Transactional
    fun updateUser(id: Long, user: User): User? {
        return if (userRepository.existsById(id)) {
            userRepository.save(user.copy(id = id))
        } else {
            null
        }
    }
    
    @Transactional
    fun deleteUser(id: Long): Boolean {
        return if (userRepository.existsById(id)) {
            userRepository.deleteById(id)
            true
        } else {
            false
        }
    }
}