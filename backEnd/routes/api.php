<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\API\TransmissionController;

Route::post('/sync', [TransmissionController::class, 'store']);