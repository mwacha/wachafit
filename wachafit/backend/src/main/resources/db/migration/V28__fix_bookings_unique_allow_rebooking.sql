-- Remove o constraint único simples que impede re-reserva após cancelamento
ALTER TABLE bookings DROP CONSTRAINT IF EXISTS bookings_schedule_id_student_id_key;

-- Adiciona índice único parcial: só proíbe duplicata se o booking não estiver cancelado
CREATE UNIQUE INDEX uq_bookings_active ON bookings (schedule_id, student_id)
    WHERE status <> 'CANCELLED';
