// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef VM_HANDLES_IMPL_H_
#define VM_HANDLES_IMPL_H_

#include "vm/heap.h"
#include "vm/visitor.h"

namespace dart {

DECLARE_DEBUG_FLAG(bool, trace_handles);

template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::VisitObjectPointers(
    ObjectPointerVisitor* visitor) {
  // Visit all zone handles.
  HandlesBlock* block = zone_blocks_;
  while (block != NULL) {
    block->VisitObjectPointers(visitor);
    block = block->next_block();
  }

  // Visit all scoped handles.
  VisitScopedHandles(visitor);
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::VisitScopedHandles(
    ObjectPointerVisitor* visitor) {
  HandlesBlock* block = &first_scoped_block_;
  do {
    block->VisitObjectPointers(visitor);
    if (block == scoped_blocks_) {
      return;
    }
    block = block->next_block();
  } while (block != NULL);
  UNREACHABLE();
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::VisitUnvisitedScopedHandles(
    ObjectPointerVisitor* visitor) {
  HandlesBlock* block = &first_scoped_block_;
  while (block != NULL && block != last_visited_block_) {
    block->VisitUnvisitedObjectPointers(visitor);
    block = block->next_block();
  }
  // We want this to point to first_scoped_block.next,
  // Because pointers are still being added to first_scoped_block
  // So it may be "partially new", and require a partial scan.
  last_visited_block_ = first_scoped_block_.next_block();
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::Visit(HandleVisitor* visitor) {
  // Visit all zone handles.
  HandlesBlock* block = zone_blocks_;
  while (block != NULL) {
    block->Visit(visitor);
    block = block->next_block();
  }

  // Visit all scoped handles.
  block = &first_scoped_block_;
  do {
    block->Visit(visitor);
    block = block->next_block();
  } while (block != NULL);
}


// Figure out the current handle scope using the current Isolate and
// allocate a handle in that scope. The function assumes that a
// current Isolate, current zone and current handle scope exist. It
// asserts for this appropriately.
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
uword Handles<kHandleSizeInWords,
              kHandlesPerChunk,
              kOffsetOfRawPtr>::AllocateHandle(Isolate* isolate) {
  ASSERT(isolate != NULL);
  ASSERT(isolate->current_zone() != NULL);
  ASSERT(isolate->top_handle_scope() != NULL);
  ASSERT(isolate->no_handle_scope_depth() == 0);
  Handles* handles = isolate->current_zone()->handles();
  ASSERT(handles != NULL);
  return handles->AllocateScopedHandle();
}


// Figure out the current zone using the current Isolate and
// allocate a handle in that zone. The function assumes that a
// current Isolate and current zone exist. It asserts for
// this appropriately.
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
uword Handles<kHandleSizeInWords,
              kHandlesPerChunk,
              kOffsetOfRawPtr>::AllocateZoneHandle(Isolate* isolate) {
  ASSERT(isolate != NULL);
  ASSERT(isolate->current_zone() != NULL);
  ASSERT(isolate->no_handle_scope_depth() == 0);
  Handles* handles = isolate->current_zone()->handles();
  ASSERT(handles != NULL);
  uword address = handles->AllocateHandleInZone();
  return address;
}


// Figure out the current zone using the current Isolate and
// check if the specified handle has been allocated in this zone.
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
bool Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::IsZoneHandle(uword handle) {
  // TODO(5411412): Accessing the current isolate is a performance problem,
  // consider passing it down as a parameter.
  Isolate* isolate = Isolate::Current();
  ASSERT(isolate != NULL);
  ASSERT(isolate->current_zone() != NULL);
  Handles* handles = isolate->current_zone()->handles();
  ASSERT(handles != NULL);
  return handles->IsValidZoneHandle(handle);
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::DeleteAll() {
  // Delete all the zone allocated handle blocks.
  // GCTrace does not need to trace this call to DeleteHandleBlocks,
  // since the individual zone deletions will be caught
  // by instrumentation in the BaseZone destructor.
  DeleteHandleBlocks(zone_blocks_);
  zone_blocks_ = NULL;

  // Delete all the scoped handle blocks.
  scoped_blocks_ = first_scoped_block_.next_block();
  DeleteHandleBlocks(scoped_blocks_);
  first_scoped_block_.ReInit();
  scoped_blocks_ = &first_scoped_block_;
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::DeleteHandleBlocks(
    HandlesBlock* blocks) {
  while (blocks != NULL) {
    HandlesBlock* block = blocks;
    blocks = blocks->next_block();
    delete block;
  }
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::SetupNextScopeBlock() {
#if defined(DEBUG)
  if (FLAG_trace_handles) {
    OS::PrintErr("***   Handle Counts for (0x%"Px"):Zone = %d,Scoped = %d\n",
                 reinterpret_cast<intptr_t>(this),
                 CountZoneHandles(), CountScopedHandles());
  }
#endif
  if (scoped_blocks_->next_block() == NULL) {
    scoped_blocks_->set_next_block(new HandlesBlock(NULL));
  }
  scoped_blocks_ = scoped_blocks_->next_block();
  scoped_blocks_->set_next_handle_slot(0);
#if defined(DEBUG)
  scoped_blocks_->ZapFreeHandles();
#endif
}


// Validation of the handle involves iterating through all the
// handle blocks to check if the handle is valid, please
// use this only in ASSERT code for verification purposes.
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
bool Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::IsValidScopedHandle(uword handle) const {
  const HandlesBlock* iterator = &first_scoped_block_;
  while (iterator != NULL) {
    if (iterator->IsValidHandle(handle)) {
      return true;
    }
    iterator = iterator->next_block();
  }
  return false;
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
bool Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::IsValidZoneHandle(uword handle) const {
  const HandlesBlock* iterator = zone_blocks_;
  while (iterator != NULL) {
    if (iterator->IsValidHandle(handle)) {
      return true;
    }
    iterator = iterator->next_block();
  }
  return false;
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::SetupNextZoneBlock() {
#if defined(DEBUG)
  if (FLAG_trace_handles) {
    OS::PrintErr("***   Handle Counts for (0x%"Px"):Zone = %d,Scoped = %d\n",
                 reinterpret_cast<intptr_t>(this),
                 CountZoneHandles(), CountScopedHandles());
  }
#endif
  zone_blocks_ = new HandlesBlock(zone_blocks_);
  ASSERT(zone_blocks_ != NULL);
}


#if defined(DEBUG)
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::VerifyScopedHandleState() {
  HandlesBlock* block = &first_scoped_block_;
  const intptr_t end_index = (kHandleSizeInWords * kHandlesPerChunk);
  do {
    if (scoped_blocks_ == block && block->next_handle_slot() <= end_index) {
      return;
    }
    block = block->next_block();
  } while (block != NULL);
  ASSERT(false);
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::ZapFreeScopedHandles() {
  HandlesBlock* block = scoped_blocks_;
  while (block != NULL) {
    block->ZapFreeHandles();
    block = block->next_block();
  }
}
#endif


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
int Handles<kHandleSizeInWords,
            kHandlesPerChunk,
            kOffsetOfRawPtr>::CountScopedHandles() const {
  int count = 0;
  const HandlesBlock* block = &first_scoped_block_;
  do {
    count += block->HandleCount();
    if (block == scoped_blocks_) {
      return count;
    }
    block = block->next_block();
  } while (block != NULL);
  UNREACHABLE();
  return 0;
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
int Handles<kHandleSizeInWords,
            kHandlesPerChunk,
            kOffsetOfRawPtr>::CountZoneHandles() const {
  int count = 0;
  const HandlesBlock* block = zone_blocks_;
  while (block != NULL) {
    count += block->HandleCount();
    block = block->next_block();
  }
  return count;
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
Handles<kHandleSizeInWords,
        kHandlesPerChunk,
        kOffsetOfRawPtr>::HandlesBlock::~HandlesBlock() {
#if defined(DEBUG)
  ReInit();
#endif
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::HandlesBlock::ReInit() {
  next_handle_slot_ = 0;
  next_block_ = NULL;
#if defined(DEBUG)
  ZapFreeHandles();
#endif
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::HandlesBlock::VisitObjectPointers(
                 ObjectPointerVisitor* visitor) {
  ASSERT(visitor != NULL);
  for (intptr_t i = 0; i < next_handle_slot_; i += kHandleSizeInWords) {
    visitor->VisitPointer(
        reinterpret_cast<RawObject**>(&data_[i + kOffsetOfRawPtr/kWordSize]));
  }
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::HandlesBlock::VisitUnvisitedObjectPointers(
                 ObjectPointerVisitor* visitor) {
  ASSERT(visitor != NULL);

  // last_visited_handle_ picks up where we were last time,
  // so there is nothing in the intialization position of this for loop.

  while (last_visited_handle_ < next_handle_slot_) {
    last_visited_handle_ += kHandleSizeInWords;
    uword* addr = &data_[last_visited_handle_ + kOffsetOfRawPtr / kWordSize];
    visitor->VisitPointer(reinterpret_cast<RawObject**>(addr));
  }
}


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::HandlesBlock::Visit(HandleVisitor* visitor) {
  ASSERT(visitor != NULL);
  for (intptr_t i = 0; i < next_handle_slot_; i += kHandleSizeInWords) {
    visitor->VisitHandle(reinterpret_cast<uword>(&data_[i]));
  }
}


#if defined(DEBUG)
template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
void Handles<kHandleSizeInWords,
             kHandlesPerChunk,
             kOffsetOfRawPtr>::HandlesBlock::ZapFreeHandles() {
  // Reinitialize the handle area to some uninitialized value.
  for (intptr_t i = next_handle_slot_;
       i < (kHandleSizeInWords * kHandlesPerChunk);
       i++) {
    data_[i] = kZapUninitializedWord;
  }
}
#endif


template <int kHandleSizeInWords, int kHandlesPerChunk, int kOffsetOfRawPtr>
int Handles<kHandleSizeInWords,
            kHandlesPerChunk,
            kOffsetOfRawPtr>::HandlesBlock::HandleCount() const {
  return (next_handle_slot_ / kHandleSizeInWords);
}

}  // namespace dart

#endif  // VM_HANDLES_IMPL_H_
