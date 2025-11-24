package com.example.demo.cinema;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CinemaRepository extends MongoRepository<Cinema, String> {}
