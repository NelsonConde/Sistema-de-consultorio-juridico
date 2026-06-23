import React from 'react';
import { useForm } from 'react-hook-form';

/**
 * Formulario de prueba para capturar el nombre del área.
 * @returns {JSX.Element} Componente de formulario.
 */
export function AreaFormTest() {
  const { register, handleSubmit, formState: { errors }, control } = useForm();

  /**
   * Maneja el envío del formulario.
   * @param {Object} data - Datos capturados por el formulario.
   * @returns {void}
   */
  const onSubmit = (data) => {
    console.log(data);
  };
  
  return (
    <div>
        <form onSubmit={handleSubmit(onSubmit)}>
        
        <label>Nombre del area</label>
        <input type="text" placeholder="area" {...register("area", {required: true})} />

        <input type="submit" />
        </form>
    </div>
  );
}