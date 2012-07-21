/*         ______   ___    ___
 *        /\  _  \ /\_ \  /\_ \
 *        \ \ \L\ \\//\ \ \//\ \      __     __   _ __   ___
 *         \ \  __ \ \ \ \  \ \ \   /'__`\ /'_ `\/\`'__\/ __`\
 *          \ \ \/\ \ \_\ \_ \_\ \_/\  __//\ \L\ \ \ \//\ \L\ \
 *           \ \_\ \_\/\____\/\____\ \____\ \____ \ \_\\ \____/
 *            \/_/\/_/\/____/\/____/\/____/\/___L\ \/_/ \/___/
 *                                           /\____/
 *                                           \_/__/
 *
 *      OpenGL implementation of some of the primitive routines.
 *
 *
 *      By Pavel Sountsov.
 *
 *      See readme.txt for copyright information.
 */

#include "allegro5/allegro.h"
#include "allegro5/allegro_primitives.h"
#include "allegro5/allegro_opengl.h"
#include "allegro5/internal/aintern_prim_opengl.h"
#include "allegro5/internal/aintern_prim_soft.h"
#include "allegro5/platform/alplatf.h"
#include "allegro5/internal/aintern_prim.h"

#ifdef ALLEGRO_CFG_OPENGL

#include "allegro5/allegro_opengl.h"
#include "allegro5/internal/aintern_opengl.h"

static void setup_state(const char* vtxs, const ALLEGRO_VERTEX_DECL* decl, ALLEGRO_BITMAP* texture)
{
   ALLEGRO_DISPLAY *display = al_get_current_display();

   if (display->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE) {
#ifndef ALLEGRO_CFG_NO_GLES2
      if(decl) {
         ALLEGRO_VERTEX_ELEMENT* e;
         e = &decl->elements[ALLEGRO_PRIM_POSITION];
         if(e->attribute) {
            int ncoord = 0;
            GLenum type = 0;

            switch(e->storage) {
               case ALLEGRO_PRIM_FLOAT_2:
                  ncoord = 2;
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_FLOAT_3:
                  ncoord = 3;
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_SHORT_2:
                  ncoord = 2;
                  type = GL_SHORT;
               break;
            }

            if (display->ogl_extras->pos_loc >= 0) {
               glVertexAttribPointer(display->ogl_extras->pos_loc, ncoord, type, false, decl->stride, vtxs + e->offset);
               glEnableVertexAttribArray(display->ogl_extras->pos_loc);
            }
         } else {
            if (display->ogl_extras->pos_loc >= 0) {
               glDisableVertexAttribArray(display->ogl_extras->pos_loc);
            }
         }

         e = &decl->elements[ALLEGRO_PRIM_TEX_COORD];
         if(!e->attribute)
            e = &decl->elements[ALLEGRO_PRIM_TEX_COORD_PIXEL];
         if(texture && e->attribute) {
            GLenum type = 0;

            switch(e->storage) {
               case ALLEGRO_PRIM_FLOAT_2:
               case ALLEGRO_PRIM_FLOAT_3:
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_SHORT_2:
                  type = GL_SHORT;
               break;
            }

            if (display->ogl_extras->texcoord_loc >= 0) {
               glVertexAttribPointer(display->ogl_extras->texcoord_loc, 2, type, false, decl->stride, vtxs + e->offset);
               glEnableVertexAttribArray(display->ogl_extras->texcoord_loc);
            }
         } else {
            if (display->ogl_extras->texcoord_loc >= 0) {
               glDisableVertexAttribArray(display->ogl_extras->texcoord_loc);
            }
         }

         e = &decl->elements[ALLEGRO_PRIM_COLOR_ATTR];
         if(e->attribute) {
            if (display->ogl_extras->color_loc >= 0) {
               glVertexAttribPointer(display->ogl_extras->color_loc, 4, GL_FLOAT, true, decl->stride, vtxs + e->offset);
               glEnableVertexAttribArray(display->ogl_extras->color_loc);
            }
         } else {
            if (display->ogl_extras->color_loc >= 0) {
               glDisableVertexAttribArray(display->ogl_extras->color_loc);
            }
         }
      } else {
         const ALLEGRO_VERTEX* vtx = (const ALLEGRO_VERTEX*)vtxs;
            
         if (display->ogl_extras->pos_loc >= 0) {
            glVertexAttribPointer(display->ogl_extras->pos_loc, 3, GL_FLOAT, false, sizeof(ALLEGRO_VERTEX), &vtx[0].x);
            glEnableVertexAttribArray(display->ogl_extras->pos_loc);
         }
         
         if (display->ogl_extras->texcoord_loc >= 0) {
            glVertexAttribPointer(display->ogl_extras->texcoord_loc, 2, GL_FLOAT, false, sizeof(ALLEGRO_VERTEX), &vtx[0].u);
            glEnableVertexAttribArray(display->ogl_extras->texcoord_loc);
         }
         
         if (display->ogl_extras->color_loc >= 0) {
            glVertexAttribPointer(display->ogl_extras->color_loc, 4, GL_FLOAT, true, sizeof(ALLEGRO_VERTEX), &vtx[0].color);
            glEnableVertexAttribArray(display->ogl_extras->color_loc);
         }
      }
#endif
   }
   else {
      if(decl) {
         ALLEGRO_VERTEX_ELEMENT* e;
         e = &decl->elements[ALLEGRO_PRIM_POSITION];
         if(e->attribute) {
            int ncoord = 0;
            GLenum type = 0;
   
            glEnableClientState(GL_VERTEX_ARRAY);
   
            switch(e->storage) {
               case ALLEGRO_PRIM_FLOAT_2:
                  ncoord = 2;
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_FLOAT_3:
                  ncoord = 3;
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_SHORT_2:
                  ncoord = 2;
                  type = GL_SHORT;
               break;
            }
            glVertexPointer(ncoord, type, decl->stride, vtxs + e->offset);
         } else {
            glDisableClientState(GL_VERTEX_ARRAY);
         }
   
         e = &decl->elements[ALLEGRO_PRIM_TEX_COORD];
         if(!e->attribute)
            e = &decl->elements[ALLEGRO_PRIM_TEX_COORD_PIXEL];
         if(texture && e->attribute) {
            GLenum type = 0;
   
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
   
            switch(e->storage) {
               case ALLEGRO_PRIM_FLOAT_2:
               case ALLEGRO_PRIM_FLOAT_3:
                  type = GL_FLOAT;
               break;
               case ALLEGRO_PRIM_SHORT_2:
                  type = GL_SHORT;
               break;
            }
            glTexCoordPointer(2, type, decl->stride, vtxs + e->offset);
         } else {
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
         }
   
         e = &decl->elements[ALLEGRO_PRIM_COLOR_ATTR];
         if(e->attribute) {
            glEnableClientState(GL_COLOR_ARRAY);
   
            glColorPointer(4, GL_FLOAT, decl->stride, vtxs + e->offset);
         } else {
            glDisableClientState(GL_COLOR_ARRAY);
            glColor4f(1, 1, 1, 1);
         }
      } else {
         const ALLEGRO_VERTEX* vtx = (const ALLEGRO_VERTEX*)vtxs;
      
         glEnableClientState(GL_COLOR_ARRAY);
         glEnableClientState(GL_VERTEX_ARRAY);
         glEnableClientState(GL_TEXTURE_COORD_ARRAY);
         if (!(display->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE))
            glDisableClientState(GL_NORMAL_ARRAY);
   
         glVertexPointer(3, GL_FLOAT, sizeof(ALLEGRO_VERTEX), &vtx[0].x);
         glColorPointer(4, GL_FLOAT, sizeof(ALLEGRO_VERTEX), &vtx[0].color.r);
         glTexCoordPointer(2, GL_FLOAT, sizeof(ALLEGRO_VERTEX), &vtx[0].u);
      }
   }

   if (texture) {
      GLuint gl_texture = al_get_opengl_texture(texture);
      int true_w, true_h;
      int tex_x, tex_y;
      float mat[4][4] = {
         {1,  0,  0, 0},
         {0, -1,  0, 0},
         {0,  0,  1, 0},
         {0,  0,  0, 1}
      };
      int height;

      if (texture->parent)
         height = texture->parent->h;
      else
         height = texture->h;
      
      al_get_opengl_texture_size(texture, &true_w, &true_h);
      al_get_opengl_texture_position(texture, &tex_x, &tex_y);
      
      mat[3][0] = (float)tex_x / true_w;
      mat[3][1] = (float)(height - tex_y) / true_h;
         
      if(decl) {
         if(decl->elements[ALLEGRO_PRIM_TEX_COORD_PIXEL].attribute) {
            mat[0][0] = 1.0f / true_w;
            mat[1][1] = -1.0f / true_h;
         } else {
            mat[0][0] = (float)al_get_bitmap_width(texture) / true_w;
            mat[1][1] = -(float)al_get_bitmap_height(texture) / true_h;
         }
      } else {
         mat[0][0] = 1.0f / true_w;
         mat[1][1] = -1.0f / true_h;
      }

      if (!(display->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE)) {
         glBindTexture(GL_TEXTURE_2D, gl_texture);
      }

      if (display->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE) {
#ifndef ALLEGRO_CFG_NO_GLES2
         float transposed[4][4];
         int x, y;
         GLint handle;
         for (y = 0; y < 4; y++) {
            for (x = 0; x < 4; x++) {
               transposed[y][x] = mat[x][y];
            }
         }

         handle = display->ogl_extras->tex_matrix_loc;
         if (handle >= 0)
            glUniformMatrix4fv(handle, 1, false, (float *)transposed);

         handle = display->ogl_extras->use_tex_matrix_loc;
         if (handle >= 0)
            glUniform1i(handle, 1);
#endif
      }
      else {
         glMatrixMode(GL_TEXTURE);
         glLoadMatrixf(mat[0]);
         glMatrixMode(GL_MODELVIEW);
      }
   } else {
      glBindTexture(GL_TEXTURE_2D, 0);
   }
}

static int draw_prim_raw(ALLEGRO_BITMAP* target, ALLEGRO_BITMAP* texture,
   const void* vtx, const ALLEGRO_VERTEX_DECL* decl,
   const int* indices, int num_vtx, int type)
{
   int num_primitives = 0;
   ALLEGRO_DISPLAY *ogl_disp = target->display;
   ALLEGRO_BITMAP *opengl_target = target;
   ALLEGRO_BITMAP_EXTRA_OPENGL *extra;
   const void* idx = indices;
   GLenum idx_size;

   #if defined ALLEGRO_GP2XWIZ || defined ALLEGRO_IPHONE
      GLushort ind[num_vtx];
      int ii;
   #endif
   
   if (target->parent) {
       opengl_target = target->parent;
   }
   extra = opengl_target->extra;
  
   if ((!extra->is_backbuffer && ogl_disp->ogl_extras->opengl_target !=
      opengl_target) || al_is_bitmap_locked(target)) {
      if(!indices)
         return _al_draw_prim_soft(texture, vtx, decl, 0, num_vtx, type);
      else
         return _al_draw_prim_indexed_soft(texture, vtx, decl, indices, num_vtx, type);
   }

   #if defined ALLEGRO_GP2XWIZ || defined ALLEGRO_IPHONE
      if (idx) {
         for (ii = 0; ii < num_vtx; ii++) {
            ind[ii] = (GLushort)indices[ii];
         }
         idx = ind;
         idx_size = GL_UNSIGNED_SHORT;
      }
   #else
      idx_size = GL_UNSIGNED_INT;
   #endif

   _al_opengl_set_blender(ogl_disp);
   setup_state(vtx, decl, texture);
   
   if(texture) {
      if (ogl_disp->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE) {
#ifndef ALLEGRO_CFG_NO_GLES2
         if (ogl_disp->ogl_extras->use_tex_loc >= 0) {
            glUniform1i(ogl_disp->ogl_extras->use_tex_loc, 1);
         }
         if (ogl_disp->ogl_extras->tex_loc >= 0) {
            glBindTexture(GL_TEXTURE_2D, al_get_opengl_texture(texture));
            glActiveTexture(GL_TEXTURE0);
            glUniform1i(ogl_disp->ogl_extras->tex_loc, 0); // 0th sampler
         }
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
#endif
      }
      else {
         glEnable(GL_TEXTURE_2D);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
         glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
      }
   }

   if(idx)
   {
      switch (type) {
         case ALLEGRO_PRIM_LINE_LIST: {
            glDrawElements(GL_LINES, num_vtx, idx_size, idx);
            num_primitives = num_vtx / 2;
            break;
         };
         case ALLEGRO_PRIM_LINE_STRIP: {
            glDrawElements(GL_LINE_STRIP, num_vtx, idx_size, idx);
            num_primitives = num_vtx - 1;
            break;
         };
         case ALLEGRO_PRIM_LINE_LOOP: {
            glDrawElements(GL_LINE_LOOP, num_vtx, idx_size, idx);
            num_primitives = num_vtx;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_LIST: {
            glDrawElements(GL_TRIANGLES, num_vtx, idx_size, idx);
            num_primitives = num_vtx / 3;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_STRIP: {
            glDrawElements(GL_TRIANGLE_STRIP, num_vtx, idx_size, idx);
            num_primitives = num_vtx - 2;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_FAN: {
            glDrawElements(GL_TRIANGLE_FAN, num_vtx, idx_size, idx);
            num_primitives = num_vtx - 2;
            break;
         };
         case ALLEGRO_PRIM_POINT_LIST: {
            glDrawElements(GL_POINTS, num_vtx, idx_size, idx);
            num_primitives = num_vtx;
            break;
         };
      }
   }
   else
   {
      switch (type) {
         case ALLEGRO_PRIM_LINE_LIST: {
            glDrawArrays(GL_LINES, 0, num_vtx);
            num_primitives = num_vtx / 2;
            break;
         };
         case ALLEGRO_PRIM_LINE_STRIP: {
            glDrawArrays(GL_LINE_STRIP, 0, num_vtx);
            num_primitives = num_vtx - 1;
            break;
         };
         case ALLEGRO_PRIM_LINE_LOOP: {
            glDrawArrays(GL_LINE_LOOP, 0, num_vtx);
            num_primitives = num_vtx;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_LIST: {
            glDrawArrays(GL_TRIANGLES, 0, num_vtx);
            num_primitives = num_vtx / 3;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_STRIP: {
            glDrawArrays(GL_TRIANGLE_STRIP, 0, num_vtx);
            num_primitives = num_vtx - 2;
            break;
         };
         case ALLEGRO_PRIM_TRIANGLE_FAN: {
            glDrawArrays(GL_TRIANGLE_FAN, 0, num_vtx);
            num_primitives = num_vtx - 2;
            break;
         };
         case ALLEGRO_PRIM_POINT_LIST: {
            glDrawArrays(GL_POINTS, 0, num_vtx);
            num_primitives = num_vtx;
            break;
         };
      }
   }

   if(texture) {
      if (ogl_disp->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE) {
#ifndef ALLEGRO_CFG_NO_GLES2
         float identity[16] = {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            0, 0, 0, 1
         };
         GLint handle;
         handle = ogl_disp->ogl_extras->tex_matrix_loc;
         if (handle >= 0)
            glUniformMatrix4fv(handle, 1, false, identity);
         handle = ogl_disp->ogl_extras->use_tex_matrix_loc;
         if (handle >= 0)
            glUniform1i(handle, 0);
         if (ogl_disp->ogl_extras->use_tex_loc >= 0)
            glUniform1i(ogl_disp->ogl_extras->use_tex_loc, 0);
#endif
      }
      else {
         glDisable(GL_TEXTURE_2D);
         glMatrixMode(GL_TEXTURE);
         glLoadIdentity();
         glMatrixMode(GL_MODELVIEW);
      }
   }

   if (ogl_disp->flags & ALLEGRO_USE_PROGRAMMABLE_PIPELINE) {
#ifndef ALLEGRO_CFG_NO_GLES2
      if (ogl_disp->ogl_extras->pos_loc >= 0)
         glDisableVertexAttribArray(ogl_disp->ogl_extras->pos_loc);
      if (ogl_disp->ogl_extras->color_loc >= 0)
         glDisableVertexAttribArray(ogl_disp->ogl_extras->color_loc);
      if (ogl_disp->ogl_extras->texcoord_loc >= 0)
         glDisableVertexAttribArray(ogl_disp->ogl_extras->texcoord_loc);
#endif
   }
   else {
      glDisableClientState(GL_COLOR_ARRAY);
      glDisableClientState(GL_VERTEX_ARRAY);
      glDisableClientState(GL_TEXTURE_COORD_ARRAY);
   }

   return num_primitives;
}

#endif /* ALLEGRO_CFG_OPENGL */

int _al_draw_prim_opengl(ALLEGRO_BITMAP* target, ALLEGRO_BITMAP* texture, const void* vtxs, const ALLEGRO_VERTEX_DECL* decl, int start, int end, int type)
{
#ifdef ALLEGRO_CFG_OPENGL
   int stride = decl ? decl->stride : (int)sizeof(ALLEGRO_VERTEX);
   return draw_prim_raw(target, texture, (const char*)vtxs + start * stride, decl, 0, end - start, type);
#else
   (void)target;
   (void)texture;
   (void)vtxs;
   (void)start;
   (void)end;
   (void)type;
   (void)decl;

   return 0;
#endif
}

int _al_draw_prim_indexed_opengl(ALLEGRO_BITMAP *target, ALLEGRO_BITMAP* texture, const void* vtxs, const ALLEGRO_VERTEX_DECL* decl, const int* indices, int num_vtx, int type)
{
#ifdef ALLEGRO_CFG_OPENGL
   return draw_prim_raw(target, texture, vtxs, decl, indices, num_vtx, type);
#else
   (void)target;
   (void)texture;
   (void)vtxs;
   (void)decl;
   (void)indices;
   (void)num_vtx;
   (void)type;

   return 0;
#endif
}

void _al_create_vertex_buffer_opengl(ALLEGRO_VERTEX_BUFFER* buf, const void* initial_data, size_t num_vertices, int usage_hints)
{
#ifdef ALLEGRO_CFG_OPENGL
   GLuint vbo;
   GLenum usage;

   switch (usage_hints)
   {
      case ALLEGRO_BUFFER_STREAM | ALLEGRO_BUFFER_DRAW:
         usage = GL_STREAM_DRAW;
         break;
      case ALLEGRO_BUFFER_STREAM | ALLEGRO_BUFFER_READ:
         usage = GL_STREAM_READ;
         break;
      case ALLEGRO_BUFFER_STREAM | ALLEGRO_BUFFER_COPY:
         usage = GL_STREAM_COPY;
         break;
      case ALLEGRO_BUFFER_STATIC | ALLEGRO_BUFFER_DRAW:
         usage = GL_STATIC_DRAW;
         break;
      case ALLEGRO_BUFFER_STATIC | ALLEGRO_BUFFER_READ:
         usage = GL_STATIC_READ;
         break;
      case ALLEGRO_BUFFER_STATIC | ALLEGRO_BUFFER_COPY:
         usage = GL_STATIC_COPY;
         break;
      case ALLEGRO_BUFFER_DYNAMIC | ALLEGRO_BUFFER_DRAW:
         usage = GL_DYNAMIC_DRAW;
         break;
      case ALLEGRO_BUFFER_DYNAMIC | ALLEGRO_BUFFER_READ:
         usage = GL_DYNAMIC_READ;
         break;
      case ALLEGRO_BUFFER_DYNAMIC | ALLEGRO_BUFFER_COPY:
         usage = GL_DYNAMIC_COPY;
         break;
      default:
         usage = GL_STATIC_DRAW;
   }

   glGenBuffers(1, &vbo);
   glBindBuffer(GL_ARRAY_BUFFER, vbo);
   glBufferData(GL_ARRAY_BUFFER, (buf->decl == 0 ? (int)sizeof(ALLEGRO_VERTEX) : buf->decl->stride) * num_vertices, initial_data, usage);
   glBindBuffer(GL_ARRAY_BUFFER, 0);

   buf->handle = vbo;
#else
   (void)buf;
   (void)decl;
   (void)initial_data;
   (void)num_vertices;
   (void)write_only;
   (void)hints;
#endif
}

void _al_destroy_vertex_buffer_opengl(ALLEGRO_VERTEX_BUFFER* buf)
{
#ifdef ALLEGRO_CFG_OPENGL
   glDeleteBuffers(1, (GLuint*)&buf->handle);
   al_free(buf->locked_memory);
#else
   (void)buf;

   return 0;
#endif
}

void _al_lock_vertex_buffer_opengl(ALLEGRO_VERTEX_BUFFER* buf)
{
#ifdef ALLEGRO_CFG_OPENGL
   int stride = buf->decl == 0 ? (int)sizeof(ALLEGRO_VERTEX) : buf->decl->stride;

   buf->locked_memory = al_realloc(buf->locked_memory, (buf->lock_end - buf->lock_start) * stride);

   if (buf->lock_flags != ALLEGRO_LOCK_WRITEONLY) {
      glBindBuffer(GL_ARRAY_BUFFER, (GLuint)buf->handle);
      glGetBufferSubData(GL_ARRAY_BUFFER, buf->lock_start * stride, (buf->lock_end - buf->lock_start) * stride, buf->locked_memory);
      glBindBuffer(GL_ARRAY_BUFFER, 0);
   }
#else
   (void)buf;
#endif
}

void _al_unlock_vertex_buffer_opengl(ALLEGRO_VERTEX_BUFFER* buf)
{
#ifdef ALLEGRO_CFG_OPENGL
   int stride = buf->decl == 0 ? (int)sizeof(ALLEGRO_VERTEX) : buf->decl->stride;

   if (buf->lock_flags != ALLEGRO_LOCK_READONLY) {
      glBindBuffer(GL_ARRAY_BUFFER, (GLuint)buf->handle);
      glBufferSubData(GL_ARRAY_BUFFER, buf->lock_start * stride, (buf->lock_end - buf->lock_start) * stride, buf->locked_memory);
      glBindBuffer(GL_ARRAY_BUFFER, 0);
   }
#else
   (void)buf;
#endif
}
