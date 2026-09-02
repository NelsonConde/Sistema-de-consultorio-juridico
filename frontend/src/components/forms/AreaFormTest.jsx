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
   * @returns {void}
   */
  const onSubmit = () => {};

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
