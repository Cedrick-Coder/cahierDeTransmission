<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\API\TransmissionController;

Route::get('/transmissions', [TransmissionController::class, 'index']);
Route::post('/sync', [TransmissionController::class, 'store']);
Route::put('/transmissions/{id}', [TransmissionController::class, 'update']);