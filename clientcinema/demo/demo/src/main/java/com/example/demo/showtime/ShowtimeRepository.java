package com.example.demo.showtime;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ShowtimeRepository extends MongoRepository<Showtime, String> {}
