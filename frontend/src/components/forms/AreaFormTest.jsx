import React from 'react';
import { useForm } from 'react-hook-form';

/**
 * Form handling.
 * @returns {JSX.Element} Result value.
 */
export function AreaFormTest() {
  const { register, handleSubmit, formState: { errors }, control } = useForm();

  /**
   * Form handling.
   * @param {Object} data - Parameter description.
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