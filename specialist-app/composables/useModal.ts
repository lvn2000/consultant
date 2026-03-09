/*
 * Copyright (c) 2026 Volodymyr Lubenchenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import { ref, type Ref } from "vue";

export interface ModalOptions {
  closeOnOverlay?: boolean;
  defaultSize?: "sm" | "md" | "lg" | "xl";
}

export interface ModalResult<T = unknown> {
  isOpen: Ref<boolean>;
  open: (data?: T) => void;
  close: () => void;
  toggle: () => void;
  modalData: Ref<T | null>;
}

export function useModal<T = unknown>(
  _options: ModalOptions = {},
): ModalResult<T> {
  const isOpen = ref(false);
  const modalData = ref<T | null>(null);

  const open = (data?: T) => {
    modalData.value = data;
    isOpen.value = true;
  };

  const close = () => {
    isOpen.value = false;
    modalData.value = null;
  };

  const toggle = () => {
    isOpen.value = !isOpen.value;
  };

  return {
    isOpen,
    open,
    close,
    toggle,
    modalData,
  };
}
